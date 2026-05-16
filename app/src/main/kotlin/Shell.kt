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
import java.io.File

class Shell(
  private val pathUtil: PathUtil = PathUtil(),
  private val shellState: ShellState = ShellState(),
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
        currentToken.append(char)
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
    return if (tokens.getOrNull(tokens.size - 2) in setOf(">", "1>")) {
      ParsedLine(
        resolveCommand(name),
        name,
        tokens.drop(1).dropLast(2),
        OutputDirection.File(tokens.last()),
        OutputDirection.Print
      )
    }
    else if (tokens.getOrNull(tokens.size - 2) == "2>") {
      ParsedLine(
        resolveCommand(name),
        name,
        tokens.drop(1).dropLast(2),
        OutputDirection.Print,
        OutputDirection.File(tokens.last())
      )
    }
    else {
      ParsedLine(
        resolveCommand(name),
        name,
        tokens.drop(1).dropLast(2),
        OutputDirection.Print,
        OutputDirection.Print
      )
    }
  }

  fun run() {
    while (true) {
      print("$ ")
      val line = readln()
      val (command, name, args, standardOutDirection, standardErrDirection) = parse(line)
      val result = command?.execute(name, args)
        ?: ExecutionResult(stderr = "$name: command not found")

      when (standardOutDirection) {
        OutputDirection.Print -> {
          result.stdout?.let(::println)
        }
        is OutputDirection.File -> {
          result.stdout?.let { File(standardOutDirection.path).writeText(it + "\n") }
        }
      }

      when (standardErrDirection) {
        OutputDirection.Print -> {
          result.stderr?.let(::println)
        }
        is OutputDirection.File -> {
          result.stderr?.let { File(standardErrDirection.path).writeText(it + "\n") }
        }
      }
    }
  }
}

enum class ParseState {
  NONE,
  OPEN_DOUBLE_QUOTE,
  OPEN_SINGLE_QUOTE,
}