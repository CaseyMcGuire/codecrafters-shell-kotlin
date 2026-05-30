package command

import ShellState
import java.io.InputStream
import java.io.PrintStream

class DeclareCommand(private val shellState: ShellState) : Command {
  override val text = "declare"
  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream
  ): Int {
    if (args.firstOrNull() == "-p") {
      val variableName = args.getOrNull(1)
      val variableValue = shellState.variables[variableName]
      if (variableValue != null) {
        stdout.println("declare -- ${variableName}=\"${variableValue}\"")
      }
      else {
        stdout.println("declare: ${variableName}: not found")
      }
    }
    else {
      args.map { it.split("=") }
        .forEach {
          val variableName = it[0]
          val variableValue = it[1]
          if (variableName.first() == '_' || variableName.first().isLetter()) {
            shellState.variables[variableName] = variableValue
          }
          else {
            stderr.println("declare: `${variableName}:${variableValue}': not a valid identifier")
          }
        }
    }
    return 0
  }
}