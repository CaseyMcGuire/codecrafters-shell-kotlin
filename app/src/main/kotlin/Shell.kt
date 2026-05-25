import command.CdCommand
import command.Command
import command.CompleteCommand
import command.EchoCommand
import command.ExecutionResult
import command.ExitCommand
import command.JobsCommand
import command.NativeCommand
import command.OutputDirection
import command.PwdCommand
import command.TypeCommand
import lib.Parser
import lib.PathUtil
import lib.TerminalReader
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class Shell(
  private val pathUtil: PathUtil = PathUtil(),
  private val shellState: ShellState = ShellState(),
) {
  private val parser: Parser = Parser()


  val builtins: List<Command> = listOf(
    EchoCommand(),
    ExitCommand(),
    TypeCommand(resolveBuiltin = ::resolveBuiltin, pathUtil = pathUtil),
    PwdCommand { shellState.currentWorkingDirectory },
    CdCommand(pathUtil, shellState),
    CompleteCommand(shellState),
    JobsCommand(shellState),
  )
  private val byText: Map<String, Command> = builtins.associateBy { it.text }

  fun resolveBuiltin(name: String): Command? = byText[name]

  fun resolveCommand(name: String): Command? =
    resolveBuiltin(name)
      ?: shellState.customCompletions[name]?.let { nativeCommand(it.toString()) }
      ?: pathUtil.getExecutablePath(name)?.let { nativeCommand(name) }

  private fun nativeCommand(name: String) =
    NativeCommand(name) { shellState.currentWorkingDirectory }

  fun parse(line: String): command.ParsedLine = parser.parse(line)

  fun run() {
    val terminalReader = TerminalReader(
      completions = pathUtil.executablesOnPath + builtins.map { it.text },
      shellState = shellState,
    )

    while (true) {
      val line = terminalReader.readLine("$ ") ?: break
      val (name, args, standardOutDirection, standardErrDirection) = parser.parse(line)
      val shouldForkProcess = args.lastOrNull() == "&"
      val result = if (shouldForkProcess) {
        val jobNumber = shellState.currentJobNumber.incrementAndGet()

        val process = ProcessBuilder( name, *args.dropLast(1).toTypedArray())
          .directory(File(shellState.currentWorkingDirectory))
          .redirectOutput(ProcessBuilder.Redirect.INHERIT)
          .redirectError(ProcessBuilder.Redirect.INHERIT)
          .start()
        val processState = ProcessState(jobNumber, process.pid(), line, ProcessStatus.RUNNING)
        shellState.jobNumberToProcess[jobNumber] = processState
        process.onExit().thenRun {
          val processState = shellState.jobNumberToProcess[jobNumber]!!
          processState.status = ProcessStatus.DONE
        }
        ExecutionResult(stdout = "[${jobNumber}] ${process.pid()}")
      }
      else {
        resolveCommand(name)?.execute(name, args)
          ?: ExecutionResult(stderr = "$name: command not found")
      }

      emit(result.stdout, standardOutDirection)
      emit(result.stderr, standardErrDirection)
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
}

data class ProcessState(val jobNumber: Int, val pid: Long, val command: String, var status: ProcessStatus)
enum class ProcessStatus(val text: String) {
  RUNNING("Running"),
  DONE("Done"),
}