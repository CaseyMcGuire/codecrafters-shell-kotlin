package command

import ShellState

class JobsCommand(private val shellState: ShellState) : Command {
  override val text: String = "jobs"
  override fun execute(name: String, args: List<String>): ExecutionResult {
    val lines = mutableListOf<String>()
    val processes = shellState.jobNumberToProcess.values.sortedBy { it.jobNumber }
    for (processState in processes) {
      val builder = StringBuilder()
      builder.append("[${processState.jobNumber}]")
      if (shellState.currentJobNumber.get() == processState.jobNumber) {
        builder.append("+")
      }
      else if (shellState.currentJobNumber.get() - 1 == processState.jobNumber) {
        builder.append("-")
      }
      else {
        builder.append(" ")
      }
      builder.append(" ")
      val padding = 24 - processState.status.name.length
      builder.append(processState.status.name.padEnd(padding))
      builder.append(processState.command)
      lines.add(builder.toString())
    }
    return ExecutionResult(stdout = if (lines.isEmpty()) null else lines.joinToString("\n"))
  }
}