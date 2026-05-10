import command.Command
import command.EchoCommand
import command.ExitCommand
import command.ParsedLine
import command.TypeCommand
import lib.PathUtil

class Shell(
  private val pathUtil: PathUtil = PathUtil(),
) {
  val builtins: List<Command> = listOf(
    EchoCommand(),
    ExitCommand(),
    TypeCommand(resolveCommand = ::resolveCommand, pathUtil = pathUtil),
  )

  private val byText: Map<String, Command> = builtins.associateBy { it.text }

  fun resolveCommand(name: String): Command? = byText[name]

  fun parse(line: String): ParsedLine {
    val tokens = line.split(" ").filter { it.isNotBlank() }
    val name = tokens.firstOrNull().orEmpty()
    return ParsedLine(resolveCommand(name), name, tokens.drop(1))
  }

  fun execute(command: String, args: List<String>): String {
    val process = ProcessBuilder(command, *args.toTypedArray())
      .redirectErrorStream(true)
      .start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    return output
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
          execute(executable, args)
        }
        else {
          "$name: command not found"
        }
      }
      output?.let(::println)
    }
  }
}