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

  fun jobs(): List<ProcessState> {
    // we do this in case there's a race condition between when the job is finished
    // and when `process.onExit()` runs
    reapJobs()
    return jobsByNumber.values.sortedBy { it.jobNumber }
  }

  fun reapJobs() {
    jobsByNumber.values.forEach { job ->
      val isAlive = ProcessHandle.of(job.pid).map { it.isAlive }.orElse(false)
        ?: false
      if (job.status == ProcessStatus.RUNNING && !isAlive) {
        markDone(job.jobNumber)
      }
    }
  }


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