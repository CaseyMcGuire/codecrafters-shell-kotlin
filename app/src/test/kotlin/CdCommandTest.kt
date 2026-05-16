import command.CdCommand
import lib.PathUtil
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CdCommandTest {

  private fun cdWith(
    initialCwd: String,
    home: String = System.getProperty("user.home"),
  ): Pair<CdCommand, ShellState> {
    val state = ShellState(currentWorkingDirectory = initialCwd, homeDirectory = home)
    return CdCommand(PathUtil(), state) to state
  }

  @Test
  fun `cd to a valid absolute path updates the working directory`(@TempDir dir: File) {
    val (cd, state) = cdWith("/")
    val result = cd.execute("cd", listOf(dir.absolutePath))

    assertNull(result.stderr)
    assertEquals(dir.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd to a missing absolute path returns an error and does not mutate state`() {
    val (cd, state) = cdWith("/")
    val missing = "/definitely_not_a_real_dir_${System.nanoTime()}"

    val result = cd.execute("cd", listOf(missing))

    assertEquals("cd: $missing: No such file or directory", result.stderr)
    assertEquals("/", state.currentWorkingDirectory)
  }

  @Test
  fun `cd with no args is a no-op`() {
    val (cd, state) = cdWith("/")
    val result = cd.execute("cd", emptyList())

    assertNull(result.stderr)
    assertEquals("/", state.currentWorkingDirectory)
  }

  @Test
  fun `cd to a relative path resolves against the current working directory`(@TempDir dir: File) {
    val sub = File(dir, "child").apply { mkdir() }
    val (cd, state) = cdWith(dir.absolutePath)

    val result = cd.execute("cd", listOf("child"))

    assertNull(result.stderr)
    assertEquals(sub.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd to dotdot moves up one directory`(@TempDir dir: File) {
    val sub = File(dir, "child").apply { mkdir() }
    val (cd, state) = cdWith(sub.absolutePath)

    val result = cd.execute("cd", listOf(".."))

    assertNull(result.stderr)
    assertEquals(dir.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd to dot stays in the same directory`(@TempDir dir: File) {
    val (cd, state) = cdWith(dir.absolutePath)

    val result = cd.execute("cd", listOf("."))

    assertNull(result.stderr)
    assertEquals(dir.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd normalizes embedded dotdot in absolute paths`(@TempDir dir: File) {
    val sub = File(dir, "child").apply { mkdir() }
    val (cd, state) = cdWith("/")

    val result = cd.execute("cd", listOf("${sub.absolutePath}/../child"))

    assertNull(result.stderr)
    assertEquals(sub.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd to a missing relative path returns an error and does not mutate state`(@TempDir dir: File) {
    val (cd, state) = cdWith(dir.absolutePath)

    val result = cd.execute("cd", listOf("nope"))

    assertEquals("cd: ${dir.absolutePath}/nope: No such file or directory", result.stderr)
    assertEquals(dir.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd ignores arguments past the first`(@TempDir first: File, @TempDir second: File) {
    val (cd, state) = cdWith("/")

    val result = cd.execute("cd", listOf(first.absolutePath, second.absolutePath))

    assertNull(result.stderr)
    assertEquals(first.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd to tilde goes to the home directory`(@TempDir home: File) {
    val (cd, state) = cdWith(initialCwd = "/", home = home.absolutePath)

    val result = cd.execute("cd", listOf("~"))

    assertNull(result.stderr)
    assertEquals(home.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd to tilde-slash-subdir resolves under the home directory`(@TempDir home: File) {
    val sub = File(home, "child").apply { mkdir() }
    val (cd, state) = cdWith(initialCwd = "/", home = home.absolutePath)

    val result = cd.execute("cd", listOf("~/child"))

    assertNull(result.stderr)
    assertEquals(sub.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd to tilde does not depend on the current working directory`(@TempDir home: File, @TempDir elsewhere: File) {
    val (cd, state) = cdWith(initialCwd = elsewhere.absolutePath, home = home.absolutePath)

    val result = cd.execute("cd", listOf("~"))

    assertNull(result.stderr)
    assertEquals(home.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd to a missing tilde path returns an error and does not mutate state`(@TempDir home: File) {
    val (cd, state) = cdWith(initialCwd = "/", home = home.absolutePath)

    val result = cd.execute("cd", listOf("~/missing"))

    val expected = "${home.absolutePath}/missing"
    assertEquals("cd: $expected: No such file or directory", result.stderr)
    assertEquals("/", state.currentWorkingDirectory)
  }

  @Test
  fun `cd does not expand tilde when not followed by slash or end-of-string`(@TempDir cwd: File, @TempDir home: File) {
    // ~bob should be treated as a literal directory name relative to cwd, NOT as user bob's home.
    val literal = File(cwd, "~bob").apply { mkdir() }
    val (cd, state) = cdWith(initialCwd = cwd.absolutePath, home = home.absolutePath)

    val result = cd.execute("cd", listOf("~bob"))

    assertNull(result.stderr)
    assertEquals(literal.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd to a missing tilde-prefixed path is reported with the literal name`(@TempDir cwd: File, @TempDir home: File) {
    val (cd, state) = cdWith(initialCwd = cwd.absolutePath, home = home.absolutePath)

    val result = cd.execute("cd", listOf("~bob"))

    assertEquals("cd: ${cwd.absolutePath}/~bob: No such file or directory", result.stderr)
    assertEquals(cwd.absolutePath, state.currentWorkingDirectory)
  }

  @Test
  fun `cd normalizes dotdot embedded after tilde expansion`(@TempDir home: File) {
    val sibling = File(home.parentFile, "sibling-${System.nanoTime()}").apply { mkdir() }
    try {
      val (cd, state) = cdWith(initialCwd = "/", home = home.absolutePath)

      val result = cd.execute("cd", listOf("~/../${sibling.name}"))

      assertNull(result.stderr)
      assertEquals(sibling.absolutePath, state.currentWorkingDirectory)
    } finally {
      sibling.delete()
    }
  }
}