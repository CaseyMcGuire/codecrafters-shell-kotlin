package command

import ShellState

class JobsCommand(private val shellState: ShellState) : Command {
  override val text: String = "jobs"
  override fun execute(name: String, args: List<String>): ExecutionResult {
    val lines = mutableListOf<String>()
    val processes = shellState.jobNumberToProcess.values.sortedBy { it.jobNumber }
    val finishedProcesses = processes.filter { it.status == ProcessStatus.DONE }
    for (processState in processes) {
      val builder = StringBuilder()
      builder.append("[${processState.jobNumber}]")
      when (processState.jobNumber) {
        shellState.currentJobNumber.get() -> {
          builder.append("+")
        }
        shellState.currentJobNumber.get() - 1 -> {
          builder.append("-")
        }
        else -> {
          builder.append(" ")
        }
      }
      builder.append(" ")
      builder.append(processState.status.name.padEnd(24))
      builder.append(processState.command)
      lines.add(builder.toString())
    }
    finishedProcesses.forEach { shellState.jobNumberToProcess.remove(it.jobNumber) }
    return ExecutionResult(stdout = if (lines.isEmpty()) null else lines.joinToString("\n"))
  }
}