package command

import lib.CustomCompletionStore
import kotlin.io.path.Path
import kotlin.io.path.exists

class CompleteCommand(
  private val customCompletionsStore: CustomCompletionStore
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
        customCompletionsStore.add(path, alias)
        return ExecutionResult(
          stdout = "complete -C '${pathString}' $alias"
        )
      }
      firstArg == "-p" -> {
        return ExecutionResult(stderr = "complete: ${args.getOrNull(1)}: no completion specification")
      }
    }
    return ExecutionResult()
  }
}