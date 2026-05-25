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
      builder.append(processState.status.text.padEnd(24))
      val commandStr = if (processState.status == ProcessStatus.DONE) {
        processState.command.takeLastWhile { it.isWhitespace() || it == '&' }
      }
      else {
        processState.command
      }
      builder.append(commandStr)
      lines.add(builder.toString())
    }
    finishedProcesses.forEach { shellState.jobNumberToProcess.remove(it.jobNumber) }
    return ExecutionResult(stdout = if (lines.isEmpty()) null else lines.joinToString("\n"))
  }
}