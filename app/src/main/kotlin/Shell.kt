import command.CdCommand
import command.Command
import command.CompleteCommand
import command.EchoCommand
import command.ExitCommand
import command.HistoryCommand
import command.JobsCommand
import command.NativeCommand
import command.OutputDirection
import command.ParsedCommand
import command.PwdCommand
import command.TypeCommand
import lib.Parser
import lib.PathUtil
import lib.TerminalReader
import org.jline.reader.History
import org.jline.reader.impl.history.DefaultHistory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import kotlin.concurrent.thread

class Shell(
  private val pathUtil: PathUtil = PathUtil(),
  private val shellState: ShellState = ShellState(),
  private val jobsManager: JobsManager = JobsManager(),
  private val doneJobCommand: Command = JobsCommand(jobsManager, doneOnly = true),
) {
  private val parser: Parser = Parser()
  private val history: History = DefaultHistory()

  init {
    val historyFile = File(System.getenv("HISTFILE"))
    if (historyFile.exists()) {
      historyFile.forEachLine { history.add(it) }
    }
  }


  val builtins: List<Command> = listOf(
    EchoCommand(),
    ExitCommand(),
    TypeCommand(resolveBuiltin = ::resolveBuiltin, pathUtil = pathUtil),
    PwdCommand { shellState.currentWorkingDirectory },
    CdCommand(pathUtil, shellState),
    CompleteCommand(shellState),
    JobsCommand(jobsManager),
    HistoryCommand(history),
  )
  private val byText: Map<String, Command> = builtins.associateBy { it.text }

  fun resolveBuiltin(name: String): Command? = byText[name]

  fun resolveCommand(name: String): Command? =
    resolveBuiltin(name)
      ?: shellState.customCompletions[name]?.let { nativeCommand(it.toString()) }
      ?: pathUtil.getExecutablePath(name)?.let { nativeCommand(name) }

  private fun nativeCommand(name: String) =
    NativeCommand(name) { shellState.currentWorkingDirectory }

  fun parse(line: String): ParsedCommand = parser.parse(line).single()

  fun runLine(line: String) = runPipeline(parser.parse(line))

  fun run() {
    val shutdownHook = Thread { cleanupJobs() }
    Runtime.getRuntime().addShutdownHook(shutdownHook)

    val terminalReader = TerminalReader(
      completions = pathUtil.executablesOnPath + builtins.map { it.text },
      shellState = shellState,
      history = history,
    )

    while (true) {
      doneJobCommand.execute("jobs", emptyList(), InputStream.nullInputStream(), System.out, System.err)
      val line = terminalReader.readLine("$ ") ?: break
      val parsedCommands = parser.parse(line)
      runPipeline(parsedCommands)
    }
  }

  private fun runPipeline(stages: List<ParsedCommand>) {
    val threads = mutableListOf<Thread>()
    var stdin: InputStream = InputStream.nullInputStream()
    for ((i, stage) in stages.withIndex()) {
      val isLast = i == stages.lastIndex
      val downstream: PipedOutputStream? = if (isLast) null else PipedOutputStream()
      val nextStdin: InputStream =
        if (downstream == null) InputStream.nullInputStream()
        else PipedInputStream(downstream, 64 * 1024)
      val stageStdin = stdin
      val stageStdout = downstream?.let { PrintStream(it, true) }

      threads += thread(isDaemon = true) {
        try {
          processLine(stage, stageStdin, stageStdout)
        } finally {
          stageStdout?.close()
        }
      }
      stdin = nextStdin
    }
    threads.forEach { it.join() }
  }

  private fun processLine(line: ParsedCommand, stdin: InputStream, stdoutOverride: PrintStream?) {
    val (name, args) = line
    val shouldForkProcess = args.lastOrNull() == "&"
    if (shouldForkProcess) {
      forkBackground(name, args.dropLast(1))
      return
    }

    // Fast path: native command writing directly to the terminal — INHERIT so the
    // child sees the real TTY instead of a JVM pipe, avoiding C stdio block-buffering.
    if (stdoutOverride == null
      && line.standardOutputDirection == OutputDirection.Print
      && resolveBuiltin(name) == null
      && pathUtil.getExecutablePath(name) != null
    ) {
      runNativeInherit(name, args, stdin, line.standardErrorDirection)
      return
    }

    val stdout = stdoutOverride ?: openStream(line.standardOutputDirection, System.out)
    val stderr = openStream(line.standardErrorDirection, System.err)
    try {
      val command = resolveCommand(name)
      if (command == null) {
        stderr.println("$name: command not found")
      } else {
        command.execute(name, args, stdin, stdout, stderr)
      }
    } finally {
      stdout.flush()
      if (stdoutOverride == null && line.standardOutputDirection is OutputDirection.File) stdout.close()
      if (line.standardErrorDirection is OutputDirection.File) stderr.close()
    }
  }

  private fun runNativeInherit(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stderrDirection: OutputDirection,
  ) {
    val process = ProcessBuilder(name, *args.toTypedArray())
      .directory(File(shellState.currentWorkingDirectory))
      .redirectOutput(ProcessBuilder.Redirect.INHERIT)
      .redirectError(redirectFor(stderrDirection))
      .start()
    thread(isDaemon = true) {
      try {
        pumpFlushed(stdin, process.outputStream)
      } catch (_: Exception) {
      } finally {
        runCatching { process.outputStream.close() }
      }
    }
    process.waitFor()
  }

  private fun forkBackground(name: String, args: List<String>) {
    val jobNumber = jobsManager.nextJobNumber()
    val process = ProcessBuilder(name, *args.toTypedArray())
      .directory(File(shellState.currentWorkingDirectory))
      .redirectOutput(ProcessBuilder.Redirect.INHERIT)
      .redirectError(ProcessBuilder.Redirect.INHERIT)
      .start()
    val displayed = (listOf(name) + args).joinToString(" ")
    jobsManager.add(ProcessState(jobNumber, process.pid(), displayed, ProcessStatus.RUNNING))
    process.onExit().thenRun { jobsManager.markDone(jobNumber) }
    println("[${jobNumber}] ${process.pid()}")
  }

  private fun openStream(direction: OutputDirection, default: PrintStream): PrintStream =
    when (direction) {
      OutputDirection.Print -> default
      is OutputDirection.File -> PrintStream(FileOutputStream(File(direction.path), direction.append), true)
    }

  // process.outputStream is a BufferedOutputStream — copyTo would let bytes sit in
  // the JVM buffer until it fills or close(). Flush after each chunk so streaming
  // pipelines (e.g. tail -f | head) actually stream.
  private fun pumpFlushed(src: InputStream, dst: java.io.OutputStream) {
    val buf = ByteArray(4096)
    while (true) {
      val n = src.read(buf)
      if (n < 0) break
      dst.write(buf, 0, n)
      dst.flush()
    }
  }

  private fun redirectFor(direction: OutputDirection): ProcessBuilder.Redirect = when (direction) {
    OutputDirection.Print -> ProcessBuilder.Redirect.INHERIT
    is OutputDirection.File ->
      if (direction.append) ProcessBuilder.Redirect.appendTo(File(direction.path))
      else ProcessBuilder.Redirect.to(File(direction.path))
  }

  private fun cleanupJobs() {
    jobsManager.destroyAliveProcesses()
  }
}
