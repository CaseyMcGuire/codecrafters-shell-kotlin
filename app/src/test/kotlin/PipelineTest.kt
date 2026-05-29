import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PipelineTest {

  private val shell = Shell()

  @AfterEach
  fun killChildren() {
    // Some tests (notably the streaming one) intentionally leave a `tail -f`
    // running. Kill any descendants spawned during the test so they don't
    // accumulate or affect later tests.
    ProcessHandle.current().descendants().forEach { it.destroyForcibly() }
  }

  private fun makeInputFile(dir: File, vararg lines: String): File =
    File(dir, "input.txt").apply {
      writeText(lines.joinToString("\n", postfix = "\n"))
    }

  @Test
  fun `builtin pipes into native — echo into wc`(@TempDir dir: File) {
    val out = File(dir, "out.txt")
    shell.runLine("echo hello | wc -c > ${out.absolutePath}")
    val content = out.readText().trim()
    assertTrue(content.contains("6"), "expected '6' in '$content' (echo+\\n = 6 bytes)")
  }

  @Test
  fun `native into native pipeline exits and emits to file`(@TempDir dir: File) {
    val input = makeInputFile(dir, "1. strawberry apple", "2. grape pear", "3. banana raspberry")
    val out = File(dir, "out.txt")
    shell.runLine("cat ${input.absolutePath} | wc > ${out.absolutePath}")
    val content = out.readText().trim()
    assertTrue(content.isNotEmpty(), "expected wc output, got empty")
    assertTrue(
      Regex("""\d+\s+\d+\s+\d+""").containsMatchIn(content),
      "expected wc-style 'lines words bytes' counts, got: '$content'",
    )
  }

  @Test
  fun `streaming pipeline emits the first line before the pipeline fully exits`(@TempDir dir: File) {
    val input = makeInputFile(dir, "1. strawberry apple", "2. grape pear", "3. banana raspberry")
    val out = File(dir, "out.txt")
    // `head -n 3` matches the file's line count so head exits cleanly and
    // flushes its file buffer — but `tail -f` keeps running, so this still
    // exercises the streaming code path. AfterEach will kill the lingering
    // tail. We deliberately use 3 (not 5) because in a non-TTY test context
    // head buffers its file-redirected stdout and would never flush if it
    // hadn't already hit its line limit.
    thread(isDaemon = true) {
      shell.runLine("tail -f ${input.absolutePath} | head -n 3 > ${out.absolutePath}")
    }

    val deadline = System.currentTimeMillis() + 5000
    var content = ""
    while (System.currentTimeMillis() < deadline) {
      if (out.exists()) {
        content = out.readText()
        if (content.contains("1. strawberry apple")) break
      }
      Thread.sleep(50)
    }
    assertTrue(
      content.contains("1. strawberry apple"),
      "expected first line within timeout, got: '$content'",
    )
  }

  @Test
  fun `pipeline redirects last-stage stdout to file`(@TempDir dir: File) {
    val out = File(dir, "out.txt")
    shell.runLine("echo hello | wc -c > ${out.absolutePath}")
    assertTrue(out.exists(), "expected '${out.absolutePath}' to be created")
    assertTrue(out.readText().trim().contains("6"))
  }

  @Test
  fun `builtin pipeline redirects last-stage stdout to file`(@TempDir dir: File) {
    val out = File(dir, "out.txt")
    shell.runLine("echo hello | cat > ${out.absolutePath}")
    assertEquals("hello", out.readText().trim())
  }

  @Test
  fun `pipeline redirects last-stage stderr to file`(@TempDir dir: File) {
    val err = File(dir, "err.txt")
    val missing = "/does-not-exist-${System.nanoTime()}"
    shell.runLine("echo hello | cat $missing 2> ${err.absolutePath}")
    val content = err.readText()
    assertTrue(content.isNotEmpty(), "expected stderr from missing-file cat, got empty")
    assertTrue(
      content.contains(missing) || content.lowercase().let {
        it.contains("no such") || it.contains("not found") || it.contains("cannot open")
      },
      "expected error mentioning '$missing', got: '$content'",
    )
  }

  @Test
  fun `pipeline does not leak shell debug output to stderr`(@TempDir dir: File) {
    val out = File(dir, "out.txt")
    val errBuf = ByteArrayOutputStream()
    val origErr = System.err
    System.setErr(PrintStream(errBuf, true))
    try {
      shell.runLine("echo hello | cat > ${out.absolutePath}")
    } finally {
      System.setErr(origErr)
    }
    val captured = errBuf.toString()
    assertFalse(
      captured.contains("[NATIVE") || captured.contains("[INHERIT"),
      "stderr leaked internal diagnostics: '$captured'",
    )
  }
}
