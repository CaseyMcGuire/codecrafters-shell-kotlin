import command.CdCommand
import command.Command
import command.EchoCommand
import command.ExecutionResult
import command.ExitCommand
import command.NativeCommand
import command.PwdCommand
import command.OutputDirection
import command.TypeCommand
import lib.Parser
import lib.PathUtil
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.File
import org.jline.reader.Parser as JLineParser

class Shell(
  private val pathUtil: PathUtil = PathUtil(),
  private val shellState: ShellState = ShellState(),
  private val terminal: Terminal = TerminalBuilder.builder().build(),
  private val jlineParser: JLineParser = DefaultParser().apply { escapeChars = charArrayOf() },
) {
  private val parser: Parser = Parser()

  val builtins: List<Command> = listOf(
    EchoCommand(),
    ExitCommand(),
    TypeCommand(resolveBuiltin = ::resolveBuiltin, pathUtil = pathUtil),
    PwdCommand { shellState.currentWorkingDirectory },
    CdCommand(
      pathUtil,
      shellState
    )
  )

  private val completer: StringsCompleter = StringsCompleter(pathUtil.executablesOnPath + builtins.map { it.text })
  private val byText: Map<String, Command> = builtins.associateBy { it.text }

  fun resolveBuiltin(name: String): Command? = byText[name]

  fun resolveCommand(name: String): Command? =
    resolveBuiltin(name) ?: pathUtil.getExecutablePath(name)?.let {
      NativeCommand(name) { shellState.currentWorkingDirectory }
    }

  fun parse(line: String): command.ParsedLine = parser.parse(line)

  fun run() {
    val reader = LineReaderBuilder.builder()
      .terminal(terminal)
      .completer(completer)
      .parser(jlineParser)
      //.option(LineReader.Option.MENU_COMPLETE, true)
      .option(LineReader.Option.AUTO_LIST, false)
      .option(LineReader.Option.LIST_AMBIGUOUS, true)
      .build()
    while (true) {
      val line = try {
        reader.readLine("$ ")
      } catch (_: UserInterruptException) {
        break
      }
      catch (_: EndOfFileException) {
        break
      }
      val (name, args, standardOutDirection, standardErrDirection) = parser.parse(line)
      val result = resolveCommand(name)?.execute(name, args)
        ?: ExecutionResult(stderr = "$name: command not found")

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