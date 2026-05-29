package command

import java.io.InputStream
import java.io.PrintStream
import kotlin.system.exitProcess

class ExitCommand(
  private val exit: (Int) -> Nothing = ::exitProcess,
) : Command {
  override val text = "exit"

  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream,
  ): Int {
    exit(0)
  }
}
