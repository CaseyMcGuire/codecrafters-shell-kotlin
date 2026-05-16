import command.Command
import command.EchoCommand
import command.TypeCommand
import lib.PathUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TypeCommandTest {

  private fun typeWith(
    resolveCommand: (String) -> Command? = { null },
    pathProvider: () -> String = { "" },
  ) = TypeCommand(
    resolveCommand = resolveCommand,
    pathUtil = PathUtil(pathProvider = pathProvider),
  )

  @Test
  fun `identifies a known builtin via resolveCommand`() {
    val type = typeWith(resolveCommand = { name -> if (name == "echo") EchoCommand() else null })
    assertEquals("echo is a shell builtin", type.execute("type", listOf("echo")).stdout)
  }

  @Test
  fun `identifies every registered builtin`() {
    val shell = Shell()
    val type = shell.resolveCommand("type")!!
    shell.builtins.forEach { builtin ->
      assertEquals(
        "${builtin.text} is a shell builtin",
        type.execute("type", listOf(builtin.text)).stdout,
      )
    }
  }

  @Test
  fun `reports not found when neither builtin nor PATH match`() {
    assertEquals("nope: not found", typeWith().execute("type", listOf("nope")).stdout)
  }

  @Test
  fun `with no args returns empty result`() {
    val result = typeWith().execute("type", emptyList())
    assertNull(result.stdout)
    assertNull(result.stderr)
  }

  @Test
  fun `only inspects the first arg`() {
    val type = typeWith(resolveCommand = { name -> if (name == "echo") EchoCommand() else null })
    assertEquals("echo is a shell builtin", type.execute("type", listOf("echo", "exit")).stdout)
  }

  @Test
  fun `prefers builtin over PATH executable when names collide`() {
    // PathUtil uses real PATH (echo exists at /bin/echo) but builtin should still win.
    val type = TypeCommand(
      resolveCommand = { name -> if (name == "echo") EchoCommand() else null },
      pathUtil = PathUtil(),
    )
    assertEquals("echo is a shell builtin", type.execute("type", listOf("echo")).stdout)
  }
}
