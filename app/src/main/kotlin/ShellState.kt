import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ShellState(
  var currentWorkingDirectory: String = System.getProperty("user.dir"),
  var homeDirectory: String = System.getenv("HOME") ?: System.getProperty("user.home"),
  val customCompletions: MutableMap<String, Path> = mutableMapOf(),
  val jobNumberToProcess: ConcurrentHashMap<Int, ProcessState> = ConcurrentHashMap<Int, ProcessState>(),
  var currentJobNumber: AtomicInteger = AtomicInteger(0)
)

