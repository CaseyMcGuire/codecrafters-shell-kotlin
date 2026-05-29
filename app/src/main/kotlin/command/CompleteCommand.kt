package command

import ShellState
import java.io.InputStream
import java.io.PrintStream
import kotlin.io.path.Path

class CompleteCommand(
  private val shellState: ShellState
) : Command {
  override val text = "complete"

  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream,
  ): Int {
    val firstArg = args.firstOrNull()
    when (firstArg) {
      "-C" -> {
        val pathString = args.getOrNull(1)
          ?: return error(stderr, "complete: -C: missing command path")
        val alias = args.getOrNull(2)
          ?: return error(stderr, "complete: -C: missing alias")
        shellState.customCompletions[alias] = Path(pathString)
        return 0
      }
      "-p" -> {
        val alias = args.getOrNull(1)
          ?: return error(stderr, "complete: -p: missing completion specification")
        val path = shellState.customCompletions[alias]
          ?: return error(stderr, "complete: ${args.getOrNull(1)}: no completion specification")
        stdout.println("complete -C '${path}' $alias")
        return 0
      }
      "-r" -> {
        val alias = args.getOrNull(1)
          ?: return error(stderr, "complete: -r: missing completion specification")
        shellState.customCompletions.remove(alias)
        return 0
      }
    }
    return 0
  }

  private fun error(stderr: PrintStream, message: String): Int {
    stderr.println(message)
    return 1
  }
}
