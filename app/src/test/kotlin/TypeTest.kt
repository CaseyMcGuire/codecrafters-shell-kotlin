import command.Command
import command.EchoCommand
import command.TypeCommand
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypeTest {

  private fun typeWith(
    pathProvider: () -> String = { System.getenv("PATH").orEmpty() },
    resolveCommand: (String) -> Command? = { null },
  ) = TypeCommand(resolveCommand = resolveCommand, pathProvider = pathProvider)

  @Test
  fun `identifies a known builtin via resolveCommand`() {
    val type = typeWith(resolveCommand = { name -> if (name == "echo") EchoCommand() else null })
    assertEquals("echo is a shell builtin", type.execute("type", listOf("echo")))
  }

  @Test
  fun `identifies every registered builtin`() {
    val shell = Shell()
    val type = shell.resolveCommand("type")!!
    shell.builtins.forEach { builtin ->
      assertEquals(
        "${builtin.text} is a shell builtin",
        type.execute("type", listOf(builtin.text)),
      )
    }
  }

  @Test
  fun `reports unknown command as not found`() {
    val type = typeWith(pathProvider = { "" })
    assertEquals("nope: not found", type.execute("type", listOf("nope")))
  }

  @Test
  fun `with no args returns null`() {
    assertNull(typeWith().execute("type", emptyList()))
  }

  @Test
  fun `only inspects the first arg`() {
    val type = typeWith(resolveCommand = { name -> if (name == "echo") EchoCommand() else null })
    assertEquals("echo is a shell builtin", type.execute("type", listOf("echo", "exit")))
  }

  @Test
  fun `returns the absolute path for a PATH executable`() {
    val result = typeWith().execute("type", listOf("ls"))
    assertNotNull(result, "expected a path for 'ls'")
    assertTrue(result.endsWith("/ls"), "expected path ending with /ls, got: $result")
    val file = File(result)
    assertTrue(file.isFile && file.canExecute(), "$result should be an executable file")
  }

  @Test
  fun `reports not found for a missing executable`() {
    val name = "definitely_not_a_real_command_${System.nanoTime()}"
    assertEquals("$name: not found", typeWith().execute("type", listOf(name)))
  }

  @Test
  fun `prefers builtin over PATH executable when names collide`() {
    val type = typeWith(resolveCommand = { name -> if (name == "echo") EchoCommand() else null })
    assertEquals("echo is a shell builtin", type.execute("type", listOf("echo")))
  }

  @Test
  fun `finds executable in injected PATH`(@TempDir dir: File) {
    val fake = File(dir, "myfakecmd").apply {
      writeText("#!/bin/sh\n")
      setExecutable(true)
    }
    val type = typeWith(pathProvider = { dir.absolutePath })

    assertEquals(fake.absolutePath, type.execute("type", listOf("myfakecmd")))
  }

  @Test
  fun `ignores non-executable files in PATH`(@TempDir dir: File) {
    File(dir, "notexec").apply { writeText("hi") }
    val type = typeWith(pathProvider = { dir.absolutePath })

    assertEquals("notexec: not found", type.execute("type", listOf("notexec")))
  }

  @Test
  fun `returns first match across multiple PATH dirs`(@TempDir first: File, @TempDir second: File) {
    val firstHit = File(first, "tool").apply { writeText("#!/bin/sh\n"); setExecutable(true) }
    File(second, "tool").apply { writeText("#!/bin/sh\n"); setExecutable(true) }
    val type = typeWith(pathProvider = { "${first.absolutePath}${File.pathSeparator}${second.absolutePath}" })

    assertEquals(firstHit.absolutePath, type.execute("type", listOf("tool")))
  }
}