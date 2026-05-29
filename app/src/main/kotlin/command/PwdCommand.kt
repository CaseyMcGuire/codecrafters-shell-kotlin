package command

import java.io.InputStream
import java.io.PrintStream

class PwdCommand(
  private val cwdProvider: () -> String
) : Command {
  override val text = "pwd"

  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream,
  ): Int {
    stdout.println(cwdProvider())
    return 0
  }
}
