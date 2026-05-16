package command

import kotlin.system.exitProcess

class ExitCommand(
  private val exit: (Int) -> Nothing = ::exitProcess,
) : Command {
  override val text = "exit"

  override fun execute(name: String, args: List<String>): ExecutionResult {
    exit(0)
  }
}