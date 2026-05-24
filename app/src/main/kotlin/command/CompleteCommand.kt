package command

import ShellState
import kotlin.io.path.Path
import kotlin.io.path.exists

class CompleteCommand(
  private val shellState: ShellState
) : Command {
  override val text = "complete"
  override fun execute(name: String, args: List<String>): ExecutionResult {
    val firstArg = args.firstOrNull()
    when {
      firstArg == "-C" -> {
        val pathString = args.getOrNull(1) ?: return ExecutionResult(stderr = "complete: -C: missing command path")
        val alias = args.getOrNull(2) ?: return ExecutionResult(stderr = "complete: -C: missing alias")
        val path = Path(pathString)
        if (!path.exists()) {
          //return ExecutionResult(stderr = "complete: -C: $pathString: No such file or directory")
        }
        shellState.customCompletions[alias] = path
        return ExecutionResult()
      }
      firstArg == "-p" -> {
        val alias = args.getOrNull(1) ?:
          return ExecutionResult(stderr = "complete: -p: missing completion specification")
        val path = shellState.customCompletions[alias] ?:
          return ExecutionResult(stderr = "complete: ${args.getOrNull(1)}: no completion specification")
        return ExecutionResult(stdout = "complete -C '${path}' $alias")
      }
      firstArg == "-r" -> {
        val alias = args.getOrNull(1) ?: return ExecutionResult(stderr = "complete: -r: missing completion specification")
        shellState.customCompletions.remove(alias)
        return ExecutionResult()
      }
    }
    return ExecutionResult()
  }
}