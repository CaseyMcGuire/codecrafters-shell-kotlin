import command.CdCommand
import command.Command
import command.EchoCommand
import command.ExecutionResult
import command.ExitCommand
import command.NativeCommand
import command.ParsedLine
import command.PwdCommand
import command.OutputDirection
import command.TypeCommand
import lib.PathUtil
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.Parser
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.File

class Shell(
  private val pathUtil: PathUtil = PathUtil(),
  private val shellState: ShellState = ShellState(),
  private val terminal: Terminal = TerminalBuilder.builder().build(),
  private val completer: StringsCompleter = StringsCompleter("echo", "exit"),
  private val parser: Parser = DefaultParser().apply { escapeChars = charArrayOf()}
) {
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

  private val byText: Map<String, Command> = builtins.associateBy { it.text }

  fun resolveBuiltin(name: String): Command? = byText[name]

  fun resolveCommand(name: String): Command? =
    resolveBuiltin(name) ?: pathUtil.getExecutablePath(name)?.let {
      NativeCommand(name) { shellState.currentWorkingDirectory }
    }

  fun parse(line: String): ParsedLine {
    val tokens = mutableListOf<String>()
    var currentToken = StringBuilder()
    var parseState = ParseState.NONE
    var isEscaped = false
    for (char in line) {
      if (isEscaped) {
        if (parseState == ParseState.OPEN_DOUBLE_QUOTE) {
          if (char in setOf('$', '\\', '`', '"')) {
            currentToken.append(char)
          }
          else {
            currentToken.append('\\').append(char)
          }
        }
        else {
          currentToken.append(char)
        }
        isEscaped = false
        continue
      }
      when (char) {
        '\\' -> if (parseState != ParseState.OPEN_SINGLE_QUOTE) isEscaped = true
                else currentToken.append(char)
        ' ' -> {
          if (parseState == ParseState.NONE) {
            if (currentToken.isNotEmpty()) {
              tokens.add(currentToken.toString())
              currentToken = StringBuilder()
            }
          }
          else {
            currentToken.append(char)
          }
        }
        '\'' -> {
          when (parseState) {
            ParseState.NONE -> parseState = ParseState.OPEN_SINGLE_QUOTE
            ParseState.OPEN_SINGLE_QUOTE -> parseState = ParseState.NONE
            else -> currentToken.append(char)
          }
        }
        '"' -> {
          when (parseState) {
            ParseState.NONE -> parseState = ParseState.OPEN_DOUBLE_QUOTE
            ParseState.OPEN_DOUBLE_QUOTE -> parseState = ParseState.NONE
            else -> currentToken.append(char)
          }
        }
        else -> currentToken.append(char)
      }
    }
    if (currentToken.isNotEmpty()) {
      tokens.add(currentToken.toString())
    }
    val name = tokens.firstOrNull().orEmpty()
    return if (tokens.getOrNull(tokens.size - 2) in setOf(">", "1>", "1>>", ">>")) {
      ParsedLine(
        resolveCommand(name),
        name,
        tokens.drop(1).dropLast(2),
        OutputDirection.File(tokens.last(), tokens[tokens.size - 2] in setOf("1>>", ">>")),
        OutputDirection.Print
      )
    }
    else if (tokens.getOrNull(tokens.size - 2) in setOf("2>", "2>>")) {
      ParsedLine(
        resolveCommand(name),
        name,
        tokens.drop(1).dropLast(2),
        OutputDirection.Print,
        OutputDirection.File(tokens.last(), tokens[tokens.size - 2] == "2>>")
      )
    }
    else {
      ParsedLine(
        resolveCommand(name),
        name,
        tokens.drop(1),
        OutputDirection.Print,
        OutputDirection.Print
      )
    }
  }

  fun run() {
    val reader = LineReaderBuilder.builder()
      .terminal(terminal)
      .completer(completer)
      .parser(parser)
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
      val (command, name, args, standardOutDirection, standardErrDirection) = parse(line)
      val result = command?.execute(name, args)
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

enum class ParseState {
  NONE,
  OPEN_DOUBLE_QUOTE,
  OPEN_SINGLE_QUOTE,
}