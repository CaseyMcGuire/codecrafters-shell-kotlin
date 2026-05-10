package command

import lib.PathUtil

class CdCommand(
  private val pathUtil: PathUtil,
  private val changeDir: (String) -> Unit
) : Command {
  override val text = "cd"
  override fun execute(name: String, args: List<String>): String? {
    val path = args.firstOrNull() ?: return null
    if (pathUtil.isValidPath(path)) {
      changeDir(path)
      return null
    }
    else {
      return "cd: $path: No such file or directory"
    }
  }
}