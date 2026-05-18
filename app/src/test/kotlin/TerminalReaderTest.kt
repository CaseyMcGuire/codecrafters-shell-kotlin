import lib.LineEditor
import lib.TerminalReader
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
    TerminalReader(completions = completions.toList())

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
  fun `completes against the word at the cursor in multi-word input`() {
    val reader = readerWith("echo", "exit")
    val editor = FakeLineEditor(textBeforeCursor = "echo ex")

    reader.handleTab(editor)

    assertEquals(listOf("it "), editor.insertions)
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
}