import lib.PathUtil
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ShellHistoryTest {

  private fun shellWith(histFile: String?) = Shell(pathUtil = PathUtil(), histFile = histFile)

  @Test
  fun `reads HISTFILE into history on startup`(@TempDir dir: File) {
    val histFile = File(dir, "histfile")
    histFile.writeText("echo one\necho two\n")

    val shell = shellWith(histFile.absolutePath)

    assertEquals(
      """
      |    1  echo one
      |    2  echo two
      """.trimMargin(),
      shell.historyCommand.runCaptured("history", emptyList()).stdout,
    )
  }

  @Test
  fun `startup with a missing HISTFILE leaves history empty`(@TempDir dir: File) {
    val shell = shellWith(File(dir, "does-not-exist").absolutePath)

    assertEquals("", shell.historyCommand.runCaptured("history", emptyList()).stdout)
  }

  @Test
  fun `startup with no HISTFILE configured leaves history empty`() {
    val shell = shellWith(null)

    assertEquals("", shell.historyCommand.runCaptured("history", emptyList()).stdout)
  }

  @Test
  fun `writeHistoryFile writes the current history to HISTFILE`(@TempDir dir: File) {
    val histFile = File(dir, "histfile")
    histFile.writeText("echo one\necho two\n")
    val shell = shellWith(histFile.absolutePath)

    shell.writeHistoryFile()

    assertEquals(listOf("echo one", "echo two"), histFile.readLines())
  }

  @Test
  fun `a load then write roundtrip does not duplicate HISTFILE entries`(@TempDir dir: File) {
    // Regression: writing the loaded history back used to append, duplicating every
    // entry that was read in at startup. -w overwrites, so the file is unchanged.
    val histFile = File(dir, "histfile")
    histFile.writeText("echo one\necho two\necho three\n")
    val shell = shellWith(histFile.absolutePath)

    shell.writeHistoryFile()

    assertEquals(listOf("echo one", "echo two", "echo three"), histFile.readLines())
  }

  @Test
  fun `writeHistoryFile with no HISTFILE configured is a no-op`() {
    val shell = shellWith(null)

    shell.writeHistoryFile()  // must not throw
  }
}