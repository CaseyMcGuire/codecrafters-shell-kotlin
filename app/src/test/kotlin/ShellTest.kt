import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ShellTest {

  private fun shell() = Shell(pathProvider = { System.getenv("PATH").orEmpty() })

  @Test
  fun `resolveCommand finds known builtin text`() {
    val shell = shell()
    assertNotNull(shell.resolveCommand("echo"))
    assertNotNull(shell.resolveCommand("type"))
    assertNotNull(shell.resolveCommand("exit"))
  }

  @Test
  fun `resolveCommand returns null for unknown text`() {
    assertNull(shell().resolveCommand("nope"))
  }

  @Test
  fun `parse splits tokens and resolves command`() {
    val shell = shell()
    val parsed = shell.parse("echo hello world")
    assertSame(shell.resolveCommand("echo"), parsed.command)
    assertEquals("echo", parsed.name)
    assertEquals(listOf("hello", "world"), parsed.args)
  }

  @Test
  fun `parse returns null command for unknown text but preserves name and args`() {
    val parsed = shell().parse("foo bar")
    assertNull(parsed.command)
    assertEquals("foo", parsed.name)
    assertEquals(listOf("bar"), parsed.args)
  }

  @Test
  fun `parse collapses repeated whitespace`() {
    val parsed = shell().parse("echo   a    b")
    assertEquals(listOf("a", "b"), parsed.args)
  }
}
