import java.nio.file.Path

class ShellState(
  var currentWorkingDirectory: String = System.getProperty("user.dir"),
  var homeDirectory: String = System.getenv("HOME") ?: System.getProperty("user.home"),
  val customCompletions: MutableMap<String, Path> = mutableMapOf(),
)