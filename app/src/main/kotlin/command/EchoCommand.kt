package command

import java.io.InputStream
import java.io.PrintStream

class EchoCommand : Command {
  override val text = "echo"

  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream,
  ): Int {
    stdout.println(args.joinToString(" "))
    return 0
  }
}
