package command

import ShellState

class JobsCommand(private val shellState: ShellState) : Command {
  override val text: String = "jobs"
  override fun execute(name: String, args: List<String>): ExecutionResult {
    val lines = mutableListOf<String>()
    for (entry in shellState.jobNumberToProcess) {
      val builder = StringBuilder()
      val processState = entry.value
      builder.append("[${processState.jobNumber}]")
      if (shellState.currentJobNumber.get() == processState.jobNumber) {
        builder.append("+")
      }
      builder.append("  ")
      val padding = 24 - processState.status.name.length
      builder.append(processState.status.name.padEnd(padding))
      builder.append(processState.command)
      lines.add(builder.toString())
    }
    return ExecutionResult(stdout = lines.joinToString("\n"))
  }
}