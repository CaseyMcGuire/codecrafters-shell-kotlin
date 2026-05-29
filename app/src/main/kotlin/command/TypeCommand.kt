package command

import lib.PathUtil
import java.io.InputStream
import java.io.PrintStream

class TypeCommand(
  private val resolveBuiltin: (String) -> Command?,
  private val pathUtil: PathUtil
) : Command {
  override val text = "type"

  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream,
  ): Int {
    val firstArg = args.firstOrNull() ?: return 0
    resolveBuiltin(firstArg)?.let {
      stdout.println("${it.text} is a shell builtin")
      return 0
    }
    val path = pathUtil.getExecutablePath(firstArg)
    if (path != null) {
      stdout.println(path)
      return 0
    }
    stdout.println("$firstArg: not found")
    return 1
  }
}
