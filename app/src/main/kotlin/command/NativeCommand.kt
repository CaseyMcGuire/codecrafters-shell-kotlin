package command

import java.io.File
import java.io.InputStream
import java.io.PrintStream
import kotlin.concurrent.thread

class NativeCommand(
  override val text: String,
  private val cwdProvider: () -> String,
) : Command {

  override fun execute(
    name: String,
    args: List<String>,
    stdin: InputStream,
    stdout: PrintStream,
    stderr: PrintStream,
  ): Int {
    val process = ProcessBuilder(name, *args.toTypedArray())
      .directory(File(cwdProvider()))
      .start()

    val stdinPump = thread(isDaemon = true) {
      try {
        // process.outputStream is a BufferedOutputStream; flush per chunk so
        // upstream bytes reach the child immediately instead of sitting in
        // the JVM buffer until close().
        val buf = ByteArray(4096)
        while (true) {
          val n = stdin.read(buf)
          if (n < 0) break
          process.outputStream.write(buf, 0, n)
          process.outputStream.flush()
        }
      } catch (_: Exception) {
      } finally {
        runCatching { process.outputStream.close() }
      }
    }
    val stdoutPump = thread(isDaemon = true) {
      process.inputStream.copyTo(stdout)
      stdout.flush()
    }
    val stderrPump = thread(isDaemon = true) {
      process.errorStream.copyTo(stderr)
      stderr.flush()
    }

    val exit = process.waitFor()
    stdoutPump.join()
    stderrPump.join()
    stdinPump.join(50)
    return exit
  }
}
