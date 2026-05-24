import lib.LineEditor
import lib.TerminalReader
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalReaderTest {

  private class FakeLineEditor(
    override var textBeforeCursor: String = "",
    override var wasLastBindingTab: Boolean = false,
  ) : LineEditor {
    val insertions = mutableListOf<String>()
    var bells = 0
    val listings = mutableListOf<String>()

    override fun insertAtCursor(text: String) { insertions += text }
    override fun bell() { bells++ }
    override fun listBelow(text: String) { listings += text }
  }

  private fun readerWith(vararg completions: String) =
    TerminalReader(completions = completions.toList(), shellState = ShellState())

  @Test
  fun `empty prefix rings the bell`() {
    val reader = readerWith("echo", "exit")
    val editor = FakeLineEditor(textBeforeCursor = "")

    reader.handleTab(editor)

    assertEquals(1, editor.bells)
    assertEquals(emptyList(), editor.insertions)
    assertEquals(emptyList(), editor.listings)
  }

  @Test
  fun `unique match inserts the suffix plus a trailing space`() {
    val reader = readerWith("echo", "exit")
    val editor = FakeLineEditor(textBeforeCursor = "ec")

    reader.handleTab(editor)

    assertEquals(listOf("ho "), editor.insertions)
    assertEquals(0, editor.bells)
  }

  @Test
  fun `multiple matches sharing a longer common prefix extend without trailing space`() {
    val reader = readerWith("xyz_rat", "xyz_rat_bee", "xyz_rat_bee_pig")
    val editor = FakeLineEditor(textBeforeCursor = "xyz_rat_")

    reader.handleTab(editor)

    assertEquals(listOf("bee"), editor.insertions)
    assertEquals(0, editor.bells)
  }

  @Test
  fun `does not add trailing space even when other completions exist starting with the LCP suffix`() {
    // Regression: an earlier bug looked up the LCP from the root of the trie,
    // so an unrelated command starting with the LCP characters would trigger a stray space.
    val reader = readerWith("xyz_rat", "xyz_rat_bee", "xyz_rat_bee_pig", "beep")
    val editor = FakeLineEditor(textBeforeCursor = "xyz_rat_")

    reader.handleTab(editor)

    assertEquals(listOf("bee"), editor.insertions)
  }

  @Test
  fun `LCP extends through chars before the branching point`() {
    val reader = readerWith("xyz_apple", "xyz_apricot")
    val editor = FakeLineEditor(textBeforeCursor = "xy")

    reader.handleTab(editor)

    // From "xy", matches diverge at 'p'/'r' after "xyz_ap", so the LCP suffix is "z_ap".
    assertEquals(listOf("z_ap"), editor.insertions)
    assertEquals(0, editor.bells)
    assertEquals(emptyList(), editor.listings)
  }

  @Test
  fun `LCP insertion does not ring the bell or list`() {
    val reader = readerWith("foo_bar_one", "foo_bar_two", "foo_bar_three")
    val editor = FakeLineEditor(textBeforeCursor = "foo_")

    reader.handleTab(editor)

    assertEquals(listOf("bar_"), editor.insertions)
    assertEquals(0, editor.bells)
    assertEquals(emptyList(), editor.listings)
  }

  @Test
  fun `LCP insertion does not require a prior tab`() {
    // No bell-then-extend sequence: a single Tab on an LCP-extendable prefix should
    // insert immediately, distinguishing this branch from the ambiguous-no-LCP branch.
    val reader = readerWith("alpha_one", "alpha_two")
    val editor = FakeLineEditor(textBeforeCursor = "al", wasLastBindingTab = false)

    reader.handleTab(editor)

    assertEquals(listOf("pha_"), editor.insertions)
    assertEquals(0, editor.bells)
  }

  @Test
  fun `ambiguous prefix with no further common chars rings bell on first tab`() {
    val reader = readerWith("echo", "exit")
    val editor = FakeLineEditor(textBeforeCursor = "e")

    reader.handleTab(editor)

    assertEquals(1, editor.bells)
    assertEquals(emptyList(), editor.insertions)
  }

  @Test
  fun `ambiguous prefix lists matches on second consecutive tab`() {
    val reader = readerWith("echo", "exit")
    val editor = FakeLineEditor(textBeforeCursor = "e")

    reader.handleTab(editor)                    // first Tab: bell
    editor.wasLastBindingTab = true             // simulate JLine: previous binding was Tab
    reader.handleTab(editor)                    // second Tab: list

    assertEquals(listOf("echo  exit"), editor.listings)
    assertEquals(1, editor.bells)               // only the first-tab bell
  }

  @Test
  fun `typing a non-tab key between tabs resets the two-tab sequence`() {
    val reader = readerWith("echo", "exit")
    val editor = FakeLineEditor(textBeforeCursor = "e")

    reader.handleTab(editor)                    // first Tab on "e": bell
    editor.wasLastBindingTab = false            // user typed a non-Tab key
    editor.textBeforeCursor = "ec"
    reader.handleTab(editor)                    // first Tab on "ec": should be a fresh first-Tab

    // "ec" has a unique match → goes to the insert branch, no extra bell from this Tab.
    assertEquals(listOf("ho "), editor.insertions)
    assertEquals(1, editor.bells)               // only the first-Tab bell from "e"
    assertEquals(emptyList(), editor.listings)
  }

  @Test
  fun `unknown prefix rings bell on first tab and again on second`() {
    val reader = readerWith("echo", "exit")
    val editor = FakeLineEditor(textBeforeCursor = "zzz")

    reader.handleTab(editor)
    editor.wasLastBindingTab = true
    reader.handleTab(editor)

    assertEquals(2, editor.bells)
    assertEquals(emptyList(), editor.listings)
  }

  @Test
  fun `lists matches sorted alphabetically separated by two spaces`() {
    val reader = readerWith("foo_zebra", "foo_apple", "foo_mango")
    val editor = FakeLineEditor(textBeforeCursor = "foo_")

    // foo_ resolves: matches=[foo_apple, foo_mango, foo_zebra], LCP from "foo_" = null
    // (the three diverge immediately at 'a'/'m'/'z'), so first Tab bells, second Tab lists.
    reader.handleTab(editor)
    editor.wasLastBindingTab = true
    reader.handleTab(editor)

    assertEquals(listOf("foo_apple  foo_mango  foo_zebra"), editor.listings)
  }

  private fun readerWithCwd(cwd: String, vararg completions: String) =
    TerminalReader(
      completions = completions.toList(),
      shellState = ShellState(currentWorkingDirectory = cwd),
    )

  @Test
  fun `arg-position single file match in cwd inserts the suffix with trailing space`(@TempDir cwd: File) {
    File(cwd, "report.txt").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat rep")

    reader.handleTab(editor)

    assertEquals(listOf("ort.txt "), editor.insertions)
  }

  @Test
  fun `arg-position with multiple matching files and no common prefix does not insert`(@TempDir cwd: File) {
    File(cwd, "foo_apple.txt").createNewFile()
    File(cwd, "foo_mango.txt").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat foo_")

    reader.handleTab(editor)

    assertEquals(emptyList(), editor.insertions)
  }

  @Test
  fun `arg-position with no matching files does not insert`(@TempDir cwd: File) {
    File(cwd, "other.txt").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat nope")

    reader.handleTab(editor)

    assertEquals(emptyList(), editor.insertions)
  }

  @Test
  fun `arg-position counts files and directories together when matching`(@TempDir cwd: File) {
    File(cwd, "foo_apple.txt").createNewFile()
    File(cwd, "foo_mango").mkdir()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat foo_")

    reader.handleTab(editor)

    // Both the file and the directory match "foo_" with no further common prefix,
    // so size != 1 and nothing is inserted.
    assertEquals(emptyList(), editor.insertions)
  }

  @Test
  fun `arg-position single directory match inserts with trailing slash`(@TempDir cwd: File) {
    File(cwd, "reports").mkdir()
    val reader = readerWithCwd(cwd.absolutePath, "cd")
    val editor = FakeLineEditor(textBeforeCursor = "cd rep")

    reader.handleTab(editor)

    assertEquals(listOf("orts/"), editor.insertions)
  }

  @Test
  fun `arg-position single file match still inserts with trailing space`(@TempDir cwd: File) {
    File(cwd, "report.txt").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat rep")

    reader.handleTab(editor)

    assertEquals(listOf("ort.txt "), editor.insertions)
  }

  @Test
  fun `arg-position single directory match inside a subdirectory inserts with trailing slash`(@TempDir cwd: File) {
    val sub = File(cwd, "sub").apply { mkdir() }
    File(sub, "deeper").mkdir()
    val reader = readerWithCwd(cwd.absolutePath, "cd")
    val editor = FakeLineEditor(textBeforeCursor = "cd sub/dee")

    reader.handleTab(editor)

    assertEquals(listOf("per/"), editor.insertions)
  }

  @Test
  fun `tab with cursor right after a trailing space completes files not commands`(@TempDir cwd: File) {
    // Reproduces tester #LC6: typed "tree " (with trailing space), pressed Tab.
    // cwd contains a directory "owl"; expectation is to complete the arg to "owl/".
    File(cwd, "owl").mkdir()
    val reader = readerWithCwd(cwd.absolutePath, "tree")
    val editor = FakeLineEditor(textBeforeCursor = "tree ")

    reader.handleTab(editor)

    assertEquals(listOf("owl/"), editor.insertions)
  }

  @Test
  fun `arg-position no matching files rings the bell and inserts nothing`(@TempDir cwd: File) {
    File(cwd, "other.txt").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat zzz")

    reader.handleTab(editor)

    assertEquals(1, editor.bells)
    assertEquals(emptyList(), editor.insertions)
    assertEquals(emptyList(), editor.listings)
  }

  @Test
  fun `arg-position empty cwd rings the bell and inserts nothing`(@TempDir cwd: File) {
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat anything")

    reader.handleTab(editor)

    assertEquals(1, editor.bells)
    assertEquals(emptyList(), editor.insertions)
  }

  @Test
  fun `arg-position no matching files inside a subdirectory rings the bell`(@TempDir cwd: File) {
    val sub = File(cwd, "sub").apply { mkdir() }
    File(sub, "other.txt").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat sub/zzz")

    reader.handleTab(editor)

    assertEquals(1, editor.bells)
    assertEquals(emptyList(), editor.insertions)
  }

  @Test
  fun `arg-position completes a file inside a relative subdirectory`(@TempDir cwd: File) {
    val sub = File(cwd, "sub").apply { mkdir() }
    File(sub, "report.txt").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat sub/rep")

    reader.handleTab(editor)

    assertEquals(listOf("ort.txt "), editor.insertions)
  }

  @Test
  fun `arg-position multiple matches with no common prefix ring the bell on first tab`(@TempDir cwd: File) {
    File(cwd, "foo_apple.txt").createNewFile()
    File(cwd, "foo_mango.txt").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat foo_")

    reader.handleTab(editor)

    assertEquals(1, editor.bells)
    assertEquals(emptyList(), editor.listings)
    assertEquals(emptyList(), editor.insertions)
  }

  @Test
  fun `arg-position multiple matches list on second consecutive tab`(@TempDir cwd: File) {
    File(cwd, "foo_apple.txt").createNewFile()
    File(cwd, "foo_mango.txt").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat foo_")

    reader.handleTab(editor)                  // first Tab: bell
    editor.wasLastBindingTab = true
    reader.handleTab(editor)                  // second Tab: list

    assertEquals(1, editor.listings.size)
    assertEquals(1, editor.bells)
  }

  @Test
  fun `arg-position listing shows directories with a trailing slash and files without`(@TempDir cwd: File) {
    File(cwd, "foo_apple.txt").createNewFile()
    File(cwd, "foo_mango").mkdir()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat foo_")

    reader.handleTab(editor)
    editor.wasLastBindingTab = true
    reader.handleTab(editor)

    val listing = editor.listings.single()
    val entries = listing.split(Regex(" +")).filter { it.isNotEmpty() }.toSet()
    assertEquals(setOf("foo_apple.txt", "foo_mango/"), entries)
  }

  @Test
  fun `arg-position listing is sorted alphabetically`(@TempDir cwd: File) {
    // Intentionally create out of order — listDirectoryEntries gives no ordering guarantee.
    File(cwd, "foo_zebra").createNewFile()
    File(cwd, "foo_apple").createNewFile()
    File(cwd, "foo_mango").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat foo_")

    reader.handleTab(editor)
    editor.wasLastBindingTab = true
    reader.handleTab(editor)

    val listing = editor.listings.single()
    val entries = listing.split(Regex(" +")).filter { it.isNotEmpty() }
    assertEquals(listOf("foo_apple", "foo_mango", "foo_zebra"), entries)
  }

  @Test
  fun `arg-position listing separates entries with at least one space`(@TempDir cwd: File) {
    File(cwd, "foo_apple.txt").createNewFile()
    File(cwd, "foo_mango.txt").createNewFile()
    val reader = readerWithCwd(cwd.absolutePath, "cat")
    val editor = FakeLineEditor(textBeforeCursor = "cat foo_")

    reader.handleTab(editor)
    editor.wasLastBindingTab = true
    reader.handleTab(editor)

    val listing = editor.listings.single()
    assertEquals(true, Regex("""foo_apple\.txt +foo_mango\.txt""").containsMatchIn(listing))
  }

  @Test
  fun `arg-position never falls through to command completion`(@TempDir cwd: File) {
    // "cat ec" would otherwise match the "echo" builtin if the command-completion
    // branch ran. The arg-position branch must return early.
    val reader = readerWithCwd(cwd.absolutePath, "echo")
    val editor = FakeLineEditor(textBeforeCursor = "cat ec")

    reader.handleTab(editor)

    assertEquals(emptyList(), editor.insertions)
    assertEquals(1, editor.bells)
  }

  private fun writeExecutable(dir: File, name: String, body: String): File {
    val file = File(dir, name)
    file.writeText("#!/bin/sh\n$body\n")
    file.setExecutable(true)
    return file
  }

  private fun readerWithCustomCompletion(alias: String, scriptPath: java.nio.file.Path): TerminalReader {
    val state = ShellState()
    state.customCompletions[alias] = scriptPath
    return TerminalReader(completions = listOf(alias), shellState = state)
  }

  @Test
  fun `custom command inserts script stdout with trailing space`(@TempDir tmp: File) {
    val script = writeExecutable(tmp, "completer", "echo hello")
    val reader = readerWithCustomCompletion("greet", script.toPath())
    val editor = FakeLineEditor(textBeforeCursor = "greet ")

    reader.handleTab(editor)

    assertEquals(listOf("hello "), editor.insertions)
    assertEquals(0, editor.bells)
  }

  @Test
  fun `custom command with empty stdout rings the bell and inserts nothing`(@TempDir tmp: File) {
    val script = writeExecutable(tmp, "completer", "true")
    val reader = readerWithCustomCompletion("greet", script.toPath())
    val editor = FakeLineEditor(textBeforeCursor = "greet ")

    reader.handleTab(editor)

    assertEquals(1, editor.bells)
    assertEquals(emptyList(), editor.insertions)
  }

  @Test
  fun `custom command output has trailing newlines trimmed before insertion`(@TempDir tmp: File) {
    val script = writeExecutable(tmp, "completer", "printf 'one\\ntwo\\n\\n'")
    val reader = readerWithCustomCompletion("greet", script.toPath())
    val editor = FakeLineEditor(textBeforeCursor = "greet ")

    reader.handleTab(editor)

    assertEquals(listOf("one\ntwo "), editor.insertions)
  }

  @Test
  fun `custom command receives the alias as the first argument`(@TempDir tmp: File) {
    val script = writeExecutable(tmp, "completer", "echo \"$1\"")
    val reader = readerWithCustomCompletion("greet", script.toPath())
    val editor = FakeLineEditor(textBeforeCursor = "greet ")

    reader.handleTab(editor)

    assertEquals(listOf("greet "), editor.insertions)
  }

  @Test
  fun `custom command fires on partial second word and inserts the suffix beyond what was typed`(@TempDir tmp: File) {
    // "checkout" starts with the typed "ch", so only the unmatched suffix is inserted.
    val script = writeExecutable(tmp, "completer", "echo checkout")
    val reader = readerWithCustomCompletion("greet", script.toPath())
    val editor = FakeLineEditor(textBeforeCursor = "greet ch")

    reader.handleTab(editor)

    assertEquals(listOf("eckout "), editor.insertions)
  }

  @Test
  fun `custom command passes the current word as the second argument`(@TempDir tmp: File) {
    // Script echoes $2 verbatim — confirms current word reaches the completer.
    val script = writeExecutable(tmp, "completer", "echo \"$2\"")
    val reader = readerWithCustomCompletion("greet", script.toPath())
    val editor = FakeLineEditor(textBeforeCursor = "greet ch")

    reader.handleTab(editor)

    // Script outputs "ch"; current word is "ch", so the inserted suffix is empty + " ".
    assertEquals(listOf(" "), editor.insertions)
  }

  @Test
  fun `custom command passes the previous word as the third argument`(@TempDir tmp: File) {
    // Echo $3 verbatim. With "greet foo bar", $3 should be "foo".
    val script = writeExecutable(tmp, "completer", "echo \"$3\"")
    val reader = readerWithCustomCompletion("greet", script.toPath())
    val editor = FakeLineEditor(textBeforeCursor = "greet foo bar")

    reader.handleTab(editor)

    // Script outputs "foo"; "foo".removePrefix("bar") is "foo" + " " → "foo ".
    assertEquals(listOf("foo "), editor.insertions)
  }

  @Test
  fun `custom command sets COMP_LINE env var to the text before the cursor`(@TempDir tmp: File) {
    val script = writeExecutable(tmp, "completer", "echo \"\$COMP_LINE\"")
    val reader = readerWithCustomCompletion("greet", script.toPath())
    val editor = FakeLineEditor(textBeforeCursor = "greet ")

    reader.handleTab(editor)

    // Trailing-space branch: "greet " + " " → "greet  ".
    assertEquals(listOf("greet  "), editor.insertions)
  }

  @Test
  fun `custom command sets COMP_POINT env var to the cursor offset`(@TempDir tmp: File) {
    val script = writeExecutable(tmp, "completer", "echo \"\$COMP_POINT\"")
    val reader = readerWithCustomCompletion("greet", script.toPath())
    val editor = FakeLineEditor(textBeforeCursor = "greet ")

    reader.handleTab(editor)

    // "greet " is 6 chars → COMP_POINT="6" → insertion "6 ".
    assertEquals(listOf("6 "), editor.insertions)
  }
}