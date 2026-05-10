package command

import lib.PathUtil
import java.io.File

class TypeCommand(
  private val resolveCommand: (String) -> Command?,
  private val pathUtil: PathUtil
) : Command {
  override val text = "type"

  override fun execute(name: String, args: List<String>): String? {
    val firstArg = args.firstOrNull() ?: return null
    resolveCommand(firstArg)?.let { return "${it.text} is a shell builtin" }
    return pathUtil.getExecutablePath(firstArg) ?: "$firstArg: not found"
  }
}