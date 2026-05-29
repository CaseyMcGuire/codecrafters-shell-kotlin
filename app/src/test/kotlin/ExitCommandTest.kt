import command.ExitCommand
import java.io.InputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExitCommandTest {

  private class ExitCalled(val code: Int) : RuntimeException()

  @Test
  fun `invokes exit with code 0`() {
    val exit = ExitCommand(exit = { code -> throw ExitCalled(code) })

    val thrown = assertFailsWith<ExitCalled> {
      exit.execute("exit", emptyList(), InputStream.nullInputStream(), PrintStream(System.out), PrintStream(System.err))
    }
    assertEquals(0, thrown.code)
  }
}
