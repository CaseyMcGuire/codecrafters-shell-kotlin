import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class CommandTest {

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
}