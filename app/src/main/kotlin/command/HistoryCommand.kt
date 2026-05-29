package command

import org.jline.reader.History
import java.io.File
import java.io.InputStream
import java.io.PrintStream

class HistoryCommand(private val history: History) : Command {
  override val text = "history"
  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream
  ): Int {
    if (args.firstOrNull() == "-r" && args.size >= 2) {
      File(args[1]).readLines().forEach { history.add(it) }
    }
    else if (args.firstOrNull() == "-2" && args.size >= 2) {
      val file = File(args[1])
      if (!file.exists()) {
        file.createNewFile()
      }
      for (line in history) {
        file.appendText("$line\n")
      }
    }
    else {
      val previousCommands = history.map { it.line() }
      val limit = args.firstOrNull()?.toIntOrNull() ?: previousCommands.size
      for (i in previousCommands.size - limit until previousCommands.size) {
        stdout.println("    ${i + 1}  ${previousCommands[i]}")
      }
    }
    return 0
  }

}