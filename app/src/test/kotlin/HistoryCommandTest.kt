import command.HistoryCommand
import org.jline.reader.History
import org.jline.reader.impl.history.DefaultHistory
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryCommandTest {

  private fun historyWith(vararg lines: String): Pair<HistoryCommand, History> {
    val history = DefaultHistory()
    lines.forEach { history.add(it) }
    return HistoryCommand(history) to history
  }

  @Test
  fun `with no args lists all commands numbered from one`() {
    val (history, _) = historyWith("echo hello", "pwd", "ls")
    val result = history.runCaptured("history", emptyList())

    assertEquals(
      """
      |    1  echo hello
      |    2  pwd
      |    3  ls
      """.trimMargin(),
      result.stdout,
    )
    assertEquals(0, result.exitCode)
  }

  @Test
  fun `with empty history prints nothing`() {
    val (history, _) = historyWith()
    val result = history.runCaptured("history", emptyList())

    assertEquals("", result.stdout)
    assertEquals(0, result.exitCode)
  }

  @Test
  fun `with a numeric arg lists only the last N commands`() {
    val (history, _) = historyWith("one", "two", "three", "four")
    val result = history.runCaptured("history", listOf("2"))

    assertEquals(
      """
      |    3  three
      |    4  four
      """.trimMargin(),
      result.stdout,
    )
  }

  @Test
  fun `a non-numeric arg falls back to listing everything`() {
    val (history, _) = historyWith("one", "two")
    val result = history.runCaptured("history", listOf("not-a-number"))

    assertEquals(
      """
      |    1  one
      |    2  two
      """.trimMargin(),
      result.stdout,
    )
  }

  @Test
  fun `-w writes the history to the given file`(@TempDir dir: File) {
    val (history, _) = historyWith("echo hello", "pwd")
    val target = File(dir, "histfile")

    val result = history.runCaptured("history", listOf("-w", target.absolutePath))

    assertEquals(0, result.exitCode)
    assertEquals(listOf("echo hello", "pwd"), target.readLines())
  }

  @Test
  fun `-w creates the file when it does not exist`(@TempDir dir: File) {
    val (history, _) = historyWith("one")
    val target = File(dir, "new-histfile")
    assertEquals(false, target.exists())

    history.runCaptured("history", listOf("-w", target.absolutePath))

    assertEquals(true, target.exists())
    assertEquals(listOf("one"), target.readLines())
  }

  @Test
  fun `-r reads commands from a file into the history`(@TempDir dir: File) {
    val (history, backing) = historyWith("existing")
    val source = File(dir, "histfile")
    source.writeText("from file 1\nfrom file 2\n")

    val result = history.runCaptured("history", listOf("-r", source.absolutePath))

    assertEquals(0, result.exitCode)
    assertEquals(listOf("existing", "from file 1", "from file 2"), backing.map { it.line() })
  }

  @Test
  fun `-r followed by listing shows the appended commands`(@TempDir dir: File) {
    val (history, _) = historyWith("existing")
    val source = File(dir, "histfile")
    source.writeText("loaded\n")

    history.runCaptured("history", listOf("-r", source.absolutePath))
    val result = history.runCaptured("history", emptyList())

    assertEquals(
      """
      |    1  existing
      |    2  loaded
      """.trimMargin(),
      result.stdout,
    )
  }

  @Test
  fun `-w without a filename falls back to listing`() {
    val (history, _) = historyWith("one")
    val result = history.runCaptured("history", listOf("-w"))

    assertEquals("    1  one", result.stdout)
  }
}