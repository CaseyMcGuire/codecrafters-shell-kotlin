package command

class EchoCommand : Command {
  override val text = "echo"

  override fun execute(name: String, args: List<String>): ExecutionResult =
    ExecutionResult(stdout = args.joinToString(" "))
}