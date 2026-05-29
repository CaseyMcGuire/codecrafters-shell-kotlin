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
    previousCommands.forEachIndexed { index, string -> stdout.println("    ${index + 1}  $string") }
    return 0
  }

}