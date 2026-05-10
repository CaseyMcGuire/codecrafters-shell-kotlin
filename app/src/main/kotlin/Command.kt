import java.io.File
import kotlin.system.exitProcess

data class ParsedLine(val command: Command, val name: String, val args: List<String>)

enum class Command(val text: String) {
  ECHO("echo") {
    override fun execute(name: String, args: List<String>): String? {
      return args.joinToString(" ")
    }
  },
  EXIT("exit") {
    override fun execute(name: String, args: List<String>): String? {
      exitProcess(0)
    }
  },
  TYPE("type") {
    override fun execute(name: String, args: List<String>): String? {
      val firstArg = args.firstOrNull()
        ?: return null
      Command.from(firstArg)?.text?.let {
        return "$it is a shell builtin"
      }
      return getExecutablePath(firstArg)
        ?: "$firstArg: not found"
    }

    private fun getExecutablePath(commandText: String): String? =
      pathProvider().split(File.pathSeparator)
         .map { "$it/$commandText" }
        .firstOrNull {
          val file = File(it)
          file.isFile && file.canExecute()
        }
  },
  UNKNOWN("") {
    override fun execute(name: String, args: List<String>): String? {
      return "$name: command not found"
    }
  };

  abstract fun execute(name: String, args: List<String>): String?

  companion object {
    internal var pathProvider: () -> String = { System.getenv("PATH").orEmpty() }

    private val textToCommand = entries.filter { it != UNKNOWN }
                                       .associateBy { it.text }
    fun from(text: String): Command? {
      return textToCommand[text]
    }

    fun parse(line: String): ParsedLine {
      val tokens = line.split(" ").filter { it.isNotBlank() }
      val name = tokens.firstOrNull().orEmpty()
      val command = from(name) ?: UNKNOWN
      return ParsedLine(command, name, tokens.drop(1))
    }
  }
}