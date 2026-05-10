class ShellState(
  var currentWorkingDirectory: String = System.getProperty("user.dir"),
  var homeDirectory: String = System.getProperty("user.home")
)

