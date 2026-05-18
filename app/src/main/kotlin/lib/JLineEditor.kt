package lib

import org.jline.reader.LineReader
import org.jline.terminal.Terminal

/**
 * [LineEditor] backed by a real JLine [LineReader] + [Terminal].
 */
internal class JLineEditor(
  private val reader: LineReader,
  private val terminal: Terminal,
) : LineEditor {
  override val textBeforeCursor: String
    get() = reader.buffer.upToCursor()

  override val wasLastBindingTab: Boolean
    get() = reader.lastBinding == "\t"

  override fun insertAtCursor(text: String) {
    reader.buffer.write(text)
  }

  override fun bell() {
    terminal.writer().write(BELL)
    terminal.writer().flush()
  }

  override fun listBelow(text: String) {
    val writer = terminal.writer()
    writer.println()
    writer.println(text)
    writer.flush()
    reader.callWidget(LineReader.REDRAW_LINE)
    reader.callWidget(LineReader.REDISPLAY)
  }

  companion object {
    private const val BELL = "\u0007"
  }
}