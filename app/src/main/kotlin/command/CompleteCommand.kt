package command

class CompleteCommand : Command {
  override val text = "complete"
  override fun execute(name: String, args: List<String>): ExecutionResult = ExecutionResult()
}