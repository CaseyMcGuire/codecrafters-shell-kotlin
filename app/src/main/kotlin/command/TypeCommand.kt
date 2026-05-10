package command

import java.io.File

class TypeCommand(
  private val resolveCommand: (String) -> Command?,
  private val pathProvider: () -> String,
) : Command {
  override val text = "type"

  override fun execute(name: String, args: List<String>): String? {
    val firstArg = args.firstOrNull() ?: return null
    resolveCommand(firstArg)?.let { return "${it.text} is a shell builtin" }
    return getExecutablePath(firstArg) ?: "$firstArg: not found"
  }

  private fun getExecutablePath(commandText: String): String? =
    pathProvider().split(File.pathSeparator)
      .map { "$it/$commandText" }
      .firstOrNull {
        val file = File(it)
        file.isFile && file.canExecute()
      }
}