package command

class EchoCommand : Command {
  override val text = "echo"

  override fun execute(name: String, args: List<String>): String =
    args.joinToString(" ")
}