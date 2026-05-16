package command

import lib.PathUtil

class TypeCommand(
  private val resolveBuiltin: (String) -> Command?,
  private val pathUtil: PathUtil
) : Command {
  override val text = "type"

  override fun execute(name: String, args: List<String>): ExecutionResult {
    val firstArg = args.firstOrNull() ?: return ExecutionResult()
    resolveBuiltin(firstArg)?.let {
      return ExecutionResult(stdout = "${it.text} is a shell builtin")
    }
    val path = pathUtil.getExecutablePath(firstArg)
    return if (path != null) ExecutionResult(stdout = path)
           else ExecutionResult(stdout = "$firstArg: not found")
  }
}