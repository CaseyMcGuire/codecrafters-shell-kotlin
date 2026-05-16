package command

interface Command {
  val text: String
  fun execute(name: String, args: List<String>): ExecutionResult
}

data class ExecutionResult(val stdout: String? = null, val stderr: String? = null)

data class ParsedLine(
  val name: String,
  val args: List<String>,
  val standardOutputDirection: OutputDirection,
  val standardErrorDirection: OutputDirection,
)

sealed interface OutputDirection {
  object Print : OutputDirection
  class File(val path: String, val append: Boolean) : OutputDirection
}