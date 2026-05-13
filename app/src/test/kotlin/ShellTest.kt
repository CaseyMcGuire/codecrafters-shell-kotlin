import lib.PathUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ShellTest {

  private fun shell() = Shell(pathUtil = PathUtil())

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

  @Test
  fun `parse keeps spaces inside single quotes as part of one arg`() {
    val parsed = shell().parse("echo 'hello world'")
    assertEquals("echo", parsed.name)
    assertEquals(listOf("hello world"), parsed.args)
  }

  @Test
  fun `parse preserves repeated whitespace inside single quotes`() {
    val parsed = shell().parse("echo 'a   b'")
    assertEquals(listOf("a   b"), parsed.args)
  }

  @Test
  fun `parse strips the quote characters from the output`() {
    val parsed = shell().parse("echo 'hi'")
    assertEquals(listOf("hi"), parsed.args)
  }

  @Test
  fun `parse glues unquoted and quoted runs into a single token`() {
    val parsed = shell().parse("echo abc'def ghi'jkl")
    assertEquals(listOf("abcdef ghijkl"), parsed.args)
  }

  @Test
  fun `parse handles multiple separate quoted args`() {
    val parsed = shell().parse("echo 'one two' 'three four'")
    assertEquals(listOf("one two", "three four"), parsed.args)
  }

  @Test
  fun `parse handles back-to-back quoted runs`() {
    val parsed = shell().parse("echo 'foo''bar'")
    assertEquals(listOf("foobar"), parsed.args)
  }


  @Test
  fun `parse leaves dollar and backslash literal inside single quotes`() {
    val parsed = shell().parse("echo '\$HOME \\n'")
    assertEquals(listOf("\$HOME \\n"), parsed.args)
  }

  @Test
  fun `parse handles a quoted command name`() {
    val shell = shell()
    val parsed = shell.parse("'echo' hi")
    assertSame(shell.resolveCommand("echo"), parsed.command)
    assertEquals("echo", parsed.name)
    assertEquals(listOf("hi"), parsed.args)
  }

  @Test
  fun `parse keeps spaces inside double quotes as part of one arg`() {
    val parsed = shell().parse("echo \"hello world\"")
    assertEquals("echo", parsed.name)
    assertEquals(listOf("hello world"), parsed.args)
  }

  @Test
  fun `parse preserves repeated whitespace inside double quotes`() {
    val parsed = shell().parse("echo \"a   b\"")
    assertEquals(listOf("a   b"), parsed.args)
  }

  @Test
  fun `parse strips the double quote characters from the output`() {
    val parsed = shell().parse("echo \"hi\"")
    assertEquals(listOf("hi"), parsed.args)
  }

  @Test
  fun `parse glues unquoted and double-quoted runs into a single token`() {
    val parsed = shell().parse("echo abc\"def ghi\"jkl")
    assertEquals(listOf("abcdef ghijkl"), parsed.args)
  }

  @Test
  fun `parse handles multiple separate double-quoted args`() {
    val parsed = shell().parse("echo \"one two\" \"three four\"")
    assertEquals(listOf("one two", "three four"), parsed.args)
  }

  @Test
  fun `parse handles back-to-back double-quoted runs`() {
    val parsed = shell().parse("echo \"foo\"\"bar\"")
    assertEquals(listOf("foobar"), parsed.args)
  }

  @Test
  fun `parse treats single quote as literal inside double quotes`() {
    val parsed = shell().parse("echo \"it's fine\"")
    assertEquals(listOf("it's fine"), parsed.args)
  }

  @Test
  fun `parse treats double quote as literal inside single quotes`() {
    val parsed = shell().parse("echo 'say \"hi\"'")
    assertEquals(listOf("say \"hi\""), parsed.args)
  }

  @Test
  fun `parse glues single-quoted and double-quoted runs together`() {
    val parsed = shell().parse("echo 'foo'\"bar\"")
    assertEquals(listOf("foobar"), parsed.args)
  }

  @Test
  fun `parse handles a double-quoted command name`() {
    val shell = shell()
    val parsed = shell.parse("\"echo\" hi")
    assertSame(shell.resolveCommand("echo"), parsed.command)
    assertEquals("echo", parsed.name)
    assertEquals(listOf("hi"), parsed.args)
  }
}
