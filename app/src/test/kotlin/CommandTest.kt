import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CommandTest {

  @AfterEach
  fun restorePathProvider() {
    Command.pathProvider = { System.getenv("PATH").orEmpty() }
  }

  @Test
  fun `echo joins args with single space`() {
    assertEquals("hello world", Command.ECHO.execute("echo", listOf("hello", "world")))
  }

  @Test
  fun `echo with no args returns empty string`() {
    assertEquals("", Command.ECHO.execute("echo", emptyList()))
  }

  @Test
  fun `unknown formats command name from name parameter`() {
    assertEquals("foo: command not found", Command.UNKNOWN.execute("foo", listOf("bar")))
  }

  @Test
  fun `from resolves known builtin text`() {
    assertSame(Command.ECHO, Command.from("echo"))
    assertSame(Command.TYPE, Command.from("type"))
    assertSame(Command.EXIT, Command.from("exit"))
  }

  @Test
  fun `from returns null for unknown text`() {
    assertNull(Command.from("nope"))
  }

  @Test
  fun `parse splits tokens and resolves command`() {
    val parsed = Command.parse("echo hello world")
    assertSame(Command.ECHO, parsed.command)
    assertEquals("echo", parsed.name)
    assertEquals(listOf("hello", "world"), parsed.args)
  }

  @Test
  fun `parse falls back to UNKNOWN preserving original name`() {
    val parsed = Command.parse("foo bar")
    assertSame(Command.UNKNOWN, parsed.command)
    assertEquals("foo", parsed.name)
    assertEquals(listOf("bar"), parsed.args)
  }

  @Test
  fun `parse collapses repeated whitespace`() {
    val parsed = Command.parse("echo   a    b")
    assertEquals(listOf("a", "b"), parsed.args)
  }

  @Test
  fun `type identifies a known builtin`() {
    Command.entries
      .filter { it != Command.UNKNOWN }
      .forEach { builtin ->
        assertEquals(
          "${builtin.text} is a shell builtin",
          Command.TYPE.execute("type", listOf(builtin.text)),
        )
      }
  }

  @Test
  fun `type reports unknown command as not found`() {
    assertEquals("nope: not found", Command.TYPE.execute("type", listOf("nope")))
  }

  @Test
  fun `type with no args returns null`() {
    assertNull(Command.TYPE.execute("type", emptyList()))
  }

  @Test
  fun `type only inspects the first arg`() {
    assertEquals("echo is a shell builtin", Command.TYPE.execute("type", listOf("echo", "exit")))
  }

  @Test
  fun `type returns the absolute path for a PATH executable`() {
    val result = Command.TYPE.execute("type", listOf("ls"))
    assertNotNull(result, "expected a path for 'ls'")
    assertTrue(result.endsWith("/ls"), "expected path ending with /ls, got: $result")
    val file = File(result)
    assertTrue(file.isFile && file.canExecute(), "$result should be an executable file")
  }

  @Test
  fun `type reports not found for a missing executable`() {
    val name = "definitely_not_a_real_command_${System.nanoTime()}"
    assertEquals("$name: not found", Command.TYPE.execute("type", listOf(name)))
  }

  @Test
  fun `type prefers builtin over PATH executable when names collide`() {
    // 'echo' exists both as a builtin and as /bin/echo — builtin should win.
    assertEquals("echo is a shell builtin", Command.TYPE.execute("type", listOf("echo")))
  }

  @Test
  fun `type finds executable in injected PATH`(@TempDir dir: File) {
    val fake = File(dir, "myfakecmd").apply {
      writeText("#!/bin/sh\n")
      setExecutable(true)
    }
    Command.pathProvider = { dir.absolutePath }

    assertEquals(fake.absolutePath, Command.TYPE.execute("type", listOf("myfakecmd")))
  }

  @Test
  fun `type ignores non-executable files in PATH`(@TempDir dir: File) {
    File(dir, "notexec").apply { writeText("hi") }  // not executable
    Command.pathProvider = { dir.absolutePath }

    assertEquals("notexec: not found", Command.TYPE.execute("type", listOf("notexec")))
  }

  @Test
  fun `type returns first match across multiple PATH dirs`(@TempDir first: File, @TempDir second: File) {
    val firstHit = File(first, "tool").apply { writeText("#!/bin/sh\n"); setExecutable(true) }
    File(second, "tool").apply { writeText("#!/bin/sh\n"); setExecutable(true) }
    Command.pathProvider = { "${first.absolutePath}${File.pathSeparator}${second.absolutePath}" }

    assertEquals(firstHit.absolutePath, Command.TYPE.execute("type", listOf("tool")))
  }
}