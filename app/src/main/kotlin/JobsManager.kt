import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class JobsManager {
  private val jobsByNumber = ConcurrentHashMap<Int, ProcessState>()
  private val currentJobNumber = AtomicInteger(0)
  private val retiredJobNumbers = PriorityBlockingQueue<Int>()

  fun nextJobNumber(): Int =
    retiredJobNumbers.poll() ?: currentJobNumber.incrementAndGet()

  fun add(job: ProcessState) {
    jobsByNumber[job.jobNumber] = job
  }

  fun markDone(jobNumber: Int) {
    jobsByNumber[jobNumber]?.status = ProcessStatus.DONE
  }

  fun retire(jobNumber: Int) {
    jobsByNumber.remove(jobNumber)
    retiredJobNumbers.add(jobNumber)
  }

  fun jobs(): List<ProcessState> = jobsByNumber.values.sortedBy { it.jobNumber }

  fun destroyAliveProcesses() {
    jobsByNumber.values.forEach { job ->
      ProcessHandle.of(job.pid).ifPresent { handle ->
        if (handle.isAlive) handle.destroy()
      }
    }
  }
}

data class ProcessState(val jobNumber: Int, val pid: Long, val command: String, var status: ProcessStatus)
enum class ProcessStatus(val text: String) {
  RUNNING("Running"),
  DONE("Done"),
}