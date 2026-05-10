import command.ExitCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExitCommandTest {

  private class ExitCalled(val code: Int) : RuntimeException()

  @Test
  fun `invokes exit with code 0`() {
    val exit = ExitCommand(exit = { code -> throw ExitCalled(code) })

    val thrown = assertFailsWith<ExitCalled> {
      exit.execute("exit", emptyList())
    }
    assertEquals(0, thrown.code)
  }
}