import command.CdCommand
import command.Command
import command.EchoCommand
import command.ExitCommand
import command.ParsedLine
import command.PwdCommand
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
    TypeCommand(resolveCommand = ::resolveCommand, pathUtil = pathUtil),
    PwdCommand { shellState.currentWorkingDirectory },
    CdCommand(
      pathUtil,
      shellState
    )
  )

  private val byText: Map<String, Command> = builtins.associateBy { it.text }

  fun resolveCommand(name: String): Command? = byText[name]

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
    return ParsedLine(resolveCommand(name), name, tokens.drop(1))
  }

  fun execute(command: String, args: List<String>): String {
    val process = ProcessBuilder(command, *args.toTypedArray())
      .directory(File(shellState.currentWorkingDirectory))
      .redirectErrorStream(true)
      .start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    return output.trimEnd('\n')
  }

  fun run() {
    while (true) {
      print("$ ")
      val line = readln()
      val (command, name, args) = parse(line)
      val output = if (command != null) command.execute(name, args)
      else {
        val executable = pathUtil.getExecutablePath(name)
        if (executable != null) {
          execute(name, args)
        } else {
          "$name: command not found"
        }
      }
      output?.let(::println)
    }
  }
}

enum class ParseState {
  NONE,
  OPEN_DOUBLE_QUOTE,
  OPEN_SINGLE_QUOTE,
}