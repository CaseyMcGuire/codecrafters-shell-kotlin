package command

import java.io.InputStream
import java.io.PrintStream

class HistoryCommand(private val previousCommands: List<String>) : Command {
  override val text = "history"
  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream
  ): Int {
    val limit = args.firstOrNull()?.toIntOrNull() ?: previousCommands.size
    for (i in previousCommands.size - limit until previousCommands.size) {
      stdout.println("    ${i + 1}  ${previousCommands[i]}")
    }
    return 0
  }

}