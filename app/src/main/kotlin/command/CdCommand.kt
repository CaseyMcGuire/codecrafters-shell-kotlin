package command

import ShellState
import lib.PathUtil
import java.nio.file.Path

class CdCommand(
  private val pathUtil: PathUtil,
  private val shellState: ShellState
) : Command {
  override val text = "cd"
  override fun execute(name: String, args: List<String>): String? {
    val path = args.firstOrNull()?.let {
      val proposedPath = if (Path.of(it).isAbsolute) it
                         else "${shellState.currentWorkingDirectory}/$it"
      pathUtil.getAdjustedPath(proposedPath)
    } ?: return null
    if (pathUtil.isValidPath(path)) {
      shellState.currentWorkingDirectory = path
      return null
    }
    else {
      return "cd: $path: No such file or directory"
    }
  }
}