package command

class JobsCommand : Command {
  override val text: String = "jobs"
  override fun execute(name: String, args: List<String>): ExecutionResult =
    ExecutionResult()
}