import command.CdCommand
import command.Command
import command.CompleteCommand
import command.EchoCommand
import command.ExecutionResult
import command.ExitCommand
import command.JobsCommand
import command.NativeCommand
import command.OutputDirection
import command.ParsedCommand
import command.PwdCommand
import command.TypeCommand
import lib.Parser
import lib.PathUtil
import lib.TerminalReader
import java.io.File

class Shell(
  private val pathUtil: PathUtil = PathUtil(),
  private val shellState: ShellState = ShellState(),
  private val jobsManager: JobsManager = JobsManager(),
  private val doneJobCommand: Command = JobsCommand(jobsManager, doneOnly = true),
) {
  private val parser: Parser = Parser()


  val builtins: List<Command> = listOf(
    EchoCommand(),
    ExitCommand(),
    TypeCommand(resolveBuiltin = ::resolveBuiltin, pathUtil = pathUtil),
    PwdCommand { shellState.currentWorkingDirectory },
    CdCommand(pathUtil, shellState),
    CompleteCommand(shellState),
    JobsCommand(jobsManager),
  )
  private val byText: Map<String, Command> = builtins.associateBy { it.text }

  fun resolveBuiltin(name: String): Command? = byText[name]

  fun resolveCommand(name: String): Command? =
    resolveBuiltin(name)
      ?: shellState.customCompletions[name]?.let { nativeCommand(it.toString()) }
      ?: pathUtil.getExecutablePath(name)?.let { nativeCommand(name) }

  private fun nativeCommand(name: String) =
    NativeCommand(name) { shellState.currentWorkingDirectory }

  fun parse(line: String): command.ParsedCommand = parser.parse(line).single()

  fun run() {
    val shutdownHook = Thread { cleanupJobs() }
    Runtime.getRuntime().addShutdownHook(shutdownHook)

    val terminalReader = TerminalReader(
      completions = pathUtil.executablesOnPath + builtins.map { it.text },
      shellState = shellState,
    )

    while (true) {
      doneJobCommand.execute("jobs", emptyList()).stdout?.let(::println)
      val line = terminalReader.readLine("$ ") ?: break
      val parsedCommands = parser.parse(line)

      val result = if (parsedCommands.size > 1) {
        val processBuilders = parsedCommands.map {
          ProcessBuilder(it.name, *it.args.toTypedArray())
        }
        val processes = ProcessBuilder.startPipeline(
          processBuilders
        )
        val standardOut = processes.last().inputStream.bufferedReader().readText().trimEnd('\n').ifEmpty { null }
        val standardErr = processes.last().errorStream.bufferedReader().readText().trimEnd('\n').ifEmpty { null }
        processes.last().waitFor()
        ExecutionResult(standardOut, standardErr)
      }
      else {
        val parsedLine = parsedCommands.single()
        processLine(parsedLine)
      }

      emit(result.stdout, parsedCommands.last().standardOutputDirection)
      emit(result.stderr, parsedCommands.last().standardErrorDirection)
    }
  }

  private fun processLine(line: ParsedCommand): ExecutionResult {
    val (name, args) = line
    val shouldForkProcess = args.lastOrNull() == "&"
    return if (shouldForkProcess) {
      val jobNumber = jobsManager.nextJobNumber()

      val process = ProcessBuilder( name, *args.dropLast(1).toTypedArray())
        .directory(File(shellState.currentWorkingDirectory))
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
      val displayed = (listOf(name) + args).joinToString(" ")
      jobsManager.add(ProcessState(jobNumber, process.pid(), displayed, ProcessStatus.RUNNING))
      process.onExit().thenRun { jobsManager.markDone(jobNumber) }
      ExecutionResult(stdout = "[${jobNumber}] ${process.pid()}")
    }
    else {
      resolveCommand(name)?.execute(name, args)
        ?: ExecutionResult(stderr = "$name: command not found")
    }

  }

  private fun emit(content: String?, direction: OutputDirection) {
    when (direction) {
      OutputDirection.Print -> content?.let(::println)
      is OutputDirection.File -> {
        val text = content?.let { "$it\n" } ?: ""
        val file = File(direction.path)
        if (direction.append) file.appendText(text) else file.writeText(text)
      }
    }
  }

  private fun cleanupJobs() {
    jobsManager.destroyAliveProcesses()
  }
}