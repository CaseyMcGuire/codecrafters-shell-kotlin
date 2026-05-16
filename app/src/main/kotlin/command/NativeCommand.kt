package command

import java.io.File
import java.util.concurrent.Executors

class NativeCommand(
  override val text: String,
  private val cwdProvider: () -> String,
) : Command {

  override fun execute(name: String, args: List<String>): ExecutionResult {
    val process = ProcessBuilder(name, *args.toTypedArray())
      .directory(File(cwdProvider()))
      .start()

    return Executors.newVirtualThreadPerTaskExecutor().use { exec ->
      val stdout = exec.submit<String?> {
        process.inputStream.bufferedReader().readText().trimEnd('\n').ifEmpty { null }
      }
      val stderr = exec.submit<String?> {
        process.errorStream.bufferedReader().readText().trimEnd('\n').ifEmpty { null }
      }
      process.waitFor()
      ExecutionResult(stdout.get(), stderr.get())
    }
  }
}
