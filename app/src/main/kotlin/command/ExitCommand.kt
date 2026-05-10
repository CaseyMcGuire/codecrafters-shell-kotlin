package command

import kotlin.system.exitProcess

class ExitCommand : Command {
  override val text = "exit"

  override fun execute(name: String, args: List<String>): String? {
    exitProcess(0)
  }
}
