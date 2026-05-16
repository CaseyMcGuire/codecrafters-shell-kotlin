package command

import ShellState
import lib.PathUtil
import java.nio.file.Path

class CdCommand(
  private val pathUtil: PathUtil,
  private val shellState: ShellState
) : Command {
  override val text = "cd"

  override fun execute(name: String, args: List<String>): ExecutionResult {
    val path = args.firstOrNull()?.let {
      if (Path.of(it).isAbsolute)
        Path.of(it).normalize().toString()
      else if (it.startsWith("~/") || it == "~")
        Path.of(it.replaceFirst("~", shellState.homeDirectory)).normalize().toString()
      else
        Path.of(shellState.currentWorkingDirectory).resolve(it).normalize().toString()
    } ?: return ExecutionResult()

    return if (pathUtil.isValidPath(path)) {
      shellState.currentWorkingDirectory = path
      ExecutionResult()
    } else {
      ExecutionResult(stderr = "cd: $path: No such file or directory")
    }
  }
}