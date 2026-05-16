package command

interface Command {
  val text: String
  fun execute(name: String, args: List<String>): ExecutionResult
}

data class ExecutionResult(val stdout: String? = null, val stderr: String? = null)

data class ParsedLine(
  val command: Command?,
  val name: String,
  val args: List<String>,
  val standardOutputDirection: StandardOutputDirection
)

sealed interface StandardOutputDirection {
  object Print : StandardOutputDirection
  class File(val path: String) : StandardOutputDirection
}