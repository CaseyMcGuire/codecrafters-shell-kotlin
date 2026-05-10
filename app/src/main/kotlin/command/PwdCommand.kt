package command

class PwdCommand(
  private val cwdProvider: () -> String
) : Command {
  override val text = "pwd"

  override fun execute(name: String, args: List<String>): String = cwdProvider()
}
