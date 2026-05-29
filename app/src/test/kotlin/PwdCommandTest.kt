import command.PwdCommand
import kotlin.test.Test
import kotlin.test.assertEquals

class PwdCommandTest {

  @Test
  fun `returns the value from cwdProvider`() {
    val pwd = PwdCommand(cwdProvider = { "/some/fake/dir" })
    assertEquals("/some/fake/dir", pwd.runCaptured("pwd", emptyList()).stdout)
  }

  @Test
  fun `ignores args`() {
    val pwd = PwdCommand(cwdProvider = { "/x" })
    assertEquals("/x", pwd.runCaptured("pwd", listOf("ignored", "args")).stdout)
  }

  @Test
  fun `re-evaluates the provider on each call`() {
    var current = "/first"
    val pwd = PwdCommand(cwdProvider = { current })

    assertEquals("/first", pwd.runCaptured("pwd", emptyList()).stdout)
    current = "/second"
    assertEquals("/second", pwd.runCaptured("pwd", emptyList()).stdout)
  }
}
