package command

import java.io.InputStream
import java.io.PrintStream

interface Command {
  val text: String
  fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream,
  ): Int
}

data class ParsedCommand(
  val name: String,
  val args: List<String>,
  val standardOutputDirection: OutputDirection,
  val standardErrorDirection: OutputDirection,
)

sealed interface OutputDirection {
  object Print : OutputDirection
  class File(val path: String, val append: Boolean) : OutputDirection
}
