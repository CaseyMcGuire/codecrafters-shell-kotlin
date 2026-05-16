package command

interface Command {
  val text: String
  fun execute(name: String, args: List<String>): String?
}

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