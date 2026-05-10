import command.Command
import command.EchoCommand
import command.ExitCommand
import command.ParsedLine
import command.TypeCommand

class Shell(
  private val pathProvider: () -> String = { System.getenv("PATH").orEmpty() },
) {
  val builtins: List<Command> = listOf(
    EchoCommand(),
    ExitCommand(),
    TypeCommand(resolveCommand = ::resolveCommand, pathProvider = pathProvider),
  )

  private val byText: Map<String, Command> = builtins.associateBy { it.text }

  fun resolveCommand(name: String): Command? = byText[name]

  fun parse(line: String): ParsedLine {
    val tokens = line.split(" ").filter { it.isNotBlank() }
    val name = tokens.firstOrNull().orEmpty()
    return ParsedLine(resolveCommand(name), name, tokens.drop(1))
  }

  fun run() {
    while (true) {
      print("$ ")
      val line = readln()
      val (command, name, args) = parse(line)
      val output = if (command != null) command.execute(name, args) else "$name: command not found"
      output?.let(::println)
    }
  }
}