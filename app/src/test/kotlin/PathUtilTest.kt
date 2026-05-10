import lib.PathUtil
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathUtilTest {

  @Test
  fun `returns the absolute path for a PATH executable`() {
    val result = PathUtil().getExecutablePath("ls")
    assertNotNull(result, "expected a path for 'ls'")
    assertTrue(result.endsWith("/ls"), "expected path ending with /ls, got: $result")
    val file = File(result)
    assertTrue(file.isFile && file.canExecute(), "$result should be an executable file")
  }

  @Test
  fun `returns null for a missing executable`() {
    val name = "definitely_not_a_real_command_${System.nanoTime()}"
    assertNull(PathUtil().getExecutablePath(name))
  }

  @Test
  fun `finds executable in injected PATH`(@TempDir dir: File) {
    val fake = File(dir, "myfakecmd").apply {
      writeText("#!/bin/sh\n")
      setExecutable(true)
    }
    val util = PathUtil(pathProvider = { dir.absolutePath })

    assertEquals(fake.absolutePath, util.getExecutablePath("myfakecmd"))
  }

  @Test
  fun `ignores non-executable files in PATH`(@TempDir dir: File) {
    File(dir, "notexec").apply { writeText("hi") }
    val util = PathUtil(pathProvider = { dir.absolutePath })

    assertNull(util.getExecutablePath("notexec"))
  }

  @Test
  fun `returns first match across multiple PATH dirs`(@TempDir first: File, @TempDir second: File) {
    val firstHit = File(first, "tool").apply { writeText("#!/bin/sh\n"); setExecutable(true) }
    File(second, "tool").apply { writeText("#!/bin/sh\n"); setExecutable(true) }
    val util = PathUtil(pathProvider = { "${first.absolutePath}${File.pathSeparator}${second.absolutePath}" })

    assertEquals(firstHit.absolutePath, util.getExecutablePath("tool"))
  }
}
