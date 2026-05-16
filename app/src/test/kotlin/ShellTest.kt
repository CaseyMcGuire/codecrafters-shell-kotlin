import command.OutputDirection
import lib.PathUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

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

  @Test
  fun `parse treats each backslash-space as a literal space within one token`() {
    val parsed = shell().parse("echo three\\ \\ \\ spaces")
    assertEquals(listOf("three   spaces"), parsed.args)
  }

  @Test
  fun `parse keeps escaped space attached but collapses subsequent unescaped spaces`() {
    val parsed = shell().parse("echo before\\     after")
    assertEquals(listOf("before ", "after"), parsed.args)
  }

  @Test
  fun `parse treats backslash-letter as the literal letter`() {
    val parsed = shell().parse("echo test\\nexample")
    assertEquals(listOf("testnexample"), parsed.args)
  }

  @Test
  fun `parse treats double backslash as a single literal backslash`() {
    val parsed = shell().parse("echo hello\\\\world")
    assertEquals(listOf("hello\\world"), parsed.args)
  }

  @Test
  fun `parse treats backslash-quote as a literal quote character`() {
    val parsed = shell().parse("echo \\'hello\\'")
    assertEquals(listOf("'hello'"), parsed.args)
  }

  @Test
  fun `parse without redirect defaults to Print direction`() {
    val parsed = shell().parse("echo hi")
    assertSame(OutputDirection.Print, parsed.standardOutputDirection)
  }

  @Test
  fun `parse with greater-than redirects stdout to a file`() {
    val parsed = shell().parse("echo hi > out.txt")
    val direction = parsed.standardOutputDirection
    assertIs<OutputDirection.File>(direction)
    assertEquals("out.txt", direction.path)
  }

  @Test
  fun `parse with one-greater-than is equivalent to greater-than`() {
    val parsed = shell().parse("echo hi 1> out.txt")
    val direction = parsed.standardOutputDirection
    assertIs<OutputDirection.File>(direction)
    assertEquals("out.txt", direction.path)
  }

  @Test
  fun `parse with redirect strips the redirect tokens from args`() {
    val parsed = shell().parse("echo hi > out.txt")
    assertEquals("echo", parsed.name)
    assertEquals(listOf("hi"), parsed.args)
  }

  @Test
  fun `parse with redirect and multiple args strips only the redirect tokens`() {
    val parsed = shell().parse("echo a b c > out.txt")
    assertEquals(listOf("a", "b", "c"), parsed.args)
  }

  @Test
  fun `parse handles a quoted file path in redirect`() {
    val parsed = shell().parse("echo hi > \"out file.txt\"")
    val direction = parsed.standardOutputDirection
    assertIs<OutputDirection.File>(direction)
    assertEquals("out file.txt", direction.path)
  }

  @Test
  fun `parse keeps a literal greater-than as an argument when not in redirect position`() {
    // ">" appearing anywhere other than second-to-last is a regular token.
    val parsed = shell().parse("echo hi > out.txt extra")
    assertNull(
      (parsed.standardOutputDirection as? OutputDirection.File)?.path
        ?.takeIf { it == "out.txt" },
      "should not have detected redirect when > is not second-to-last",
    )
    assertSame(OutputDirection.Print, parsed.standardOutputDirection)
    assertTrue(">" in parsed.args, "expected '>' to remain in args")
  }
}
