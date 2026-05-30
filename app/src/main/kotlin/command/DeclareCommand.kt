package command

import java.io.InputStream
import java.io.PrintStream

class DeclareCommand : Command {
  override val text = "declare"
  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream
  ): Int {
    if (args.firstOrNull() == "-p") {
      stdout.println("declare: ${args.getOrNull(1)}: not found")
    }
    return 0
  }
}