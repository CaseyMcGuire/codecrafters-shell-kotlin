package command

class PwdCommand(
  private val cwdProvider: () -> String = { System.getProperty("user.dir") },
) : Command {
  override val text = "pwd"

  override fun execute(name: String, args: List<String>): String = cwdProvider()
}
