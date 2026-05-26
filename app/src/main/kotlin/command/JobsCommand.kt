package command

import JobsManager
import ProcessStatus

class JobsCommand(
  private val jobsManager: JobsManager,
  private val doneOnly: Boolean = false
) : Command {
  override val text: String = "jobs"
  override fun execute(name: String, args: List<String>): ExecutionResult {
    val lines = mutableListOf<String>()
    val processes = jobsManager.jobs()
      .filter { !doneOnly || it.status == ProcessStatus.DONE }
    val finishedProcesses = processes.filter { it.status == ProcessStatus.DONE }
    val jobNumbers = processes.map { it.jobNumber }.sorted()

    for (processState in processes) {
      val builder = StringBuilder()
      builder.append("[${processState.jobNumber}]")
      when (processState.jobNumber) {
        jobNumbers.lastOrNull() -> {
          builder.append("+")
        }
        jobNumbers.getOrNull(jobNumbers.size - 2) -> {
          builder.append("-")
        }
        else -> {
          builder.append(" ")
        }
      }
      builder.append(" ")
      builder.append(processState.status.text.padEnd(24))
      val commandStr = if (processState.status == ProcessStatus.DONE) {
        processState.command.dropLastWhile { it.isWhitespace() || it == '&' }
      }
      else {
        processState.command
      }

      builder.append(commandStr)
      lines.add(builder.toString())
    }
    finishedProcesses.forEach { jobsManager.retire(it.jobNumber) }
    return ExecutionResult(stdout = if (lines.isEmpty()) null else lines.joinToString("\n"))
  }
}