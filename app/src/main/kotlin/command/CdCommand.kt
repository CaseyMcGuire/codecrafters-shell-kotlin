package command

import ShellState
import lib.PathUtil
import java.io.InputStream
import java.io.PrintStream
import java.nio.file.Path

class CdCommand(
  private val pathUtil: PathUtil,
  private val shellState: ShellState
) : Command {
  override val text = "cd"

  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream,
  ): Int {
    val path = args.firstOrNull()?.let {
      if (Path.of(it).isAbsolute)
        Path.of(it).normalize().toString()
      else if (it.startsWith("~/") || it == "~")
        Path.of(it.replaceFirst("~", shellState.homeDirectory)).normalize().toString()
      else
        Path.of(shellState.currentWorkingDirectory).resolve(it).normalize().toString()
    } ?: return 0

    return if (pathUtil.isValidPath(path)) {
      shellState.currentWorkingDirectory = path
      0
    } else {
      stderr.println("cd: $path: No such file or directory")
      1
    }
  }
}
