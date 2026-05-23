package command

class CompleteCommand : Command {
  override val text = "complete"
  override fun execute(name: String, args: List<String>): ExecutionResult {
    if (args.firstOrNull() == "-p") {
      // lookup builtin commands
      return ExecutionResult(stderr = "complete: ${args.getOrNull(1)}: no completion specification")
    }

    return ExecutionResult()
  }
}