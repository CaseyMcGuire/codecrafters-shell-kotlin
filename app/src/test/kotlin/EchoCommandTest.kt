import command.EchoCommand
import kotlin.test.Test
import kotlin.test.assertEquals

class EchoCommandTest {

  @Test
  fun `joins args with single space`() {
    assertEquals("hello world", EchoCommand().execute("echo", listOf("hello", "world")).stdout)
  }

  @Test
  fun `with no args returns empty string`() {
    assertEquals("", EchoCommand().execute("echo", emptyList()).stdout)
  }
}