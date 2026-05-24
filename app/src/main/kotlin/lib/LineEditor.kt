package lib

/**
 * Thin abstraction over a line-editing terminal so we can swap out JLine for tests.
 * Only exposes what the tab-completion widget needs.
 */
interface LineEditor {
  /** Everything typed on the current line from start of buffer up to the cursor. */
  val textBeforeCursor: String

  /** Whether the immediately previous key binding was Tab. Used to detect Tab→Tab repeats. */
  val wasLastBindingTab: Boolean

  /** Insert [text] at the cursor, advancing it. */
  fun insertAtCursor(text: String)

  /** Ring the terminal bell. */
  fun bell()

  /** Print [text] on a new line below the current prompt, then redraw the prompt + buffer. */
  fun listBelow(text: String)

  /** Submit the current line as if the user pressed Enter. */
  fun acceptLine()
}