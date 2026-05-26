import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class ShellState(
  var currentWorkingDirectory: String = System.getProperty("user.dir"),
  var homeDirectory: String = System.getenv("HOME") ?: System.getProperty("user.home"),
  val customCompletions: MutableMap<String, Path> = mutableMapOf(),
  val jobNumberToProcess: ConcurrentHashMap<Int, ProcessState> = ConcurrentHashMap<Int, ProcessState>(),
  val currentJobNumber: AtomicInteger = AtomicInteger(0),
  val retiredJobNumbers: PriorityBlockingQueue<Int> = PriorityBlockingQueue(),
) {
  fun getNextJobNumber(): Int {
    if (retiredJobNumbers.isNotEmpty()) {
      return retiredJobNumbers.poll()
    }
    return currentJobNumber.incrementAndGet()
  }

  fun addRetiredJobNumber(jobNumber: Int) {
    retiredJobNumbers.add(jobNumber)
  }
}

