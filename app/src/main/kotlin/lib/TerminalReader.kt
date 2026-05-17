package lib

import datastructures.Trie
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.Reference
import org.jline.reader.UserInterruptException
import org.jline.reader.Widget
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

class TerminalReader(
  completions: List<String>,
  private val terminal: Terminal = TerminalBuilder.builder().build(),
  private val trie: Trie = Trie(completions.distinct()),
) {
  private val reader: LineReader = buildReader()

  /**
   * Read one line of input from the user. Returns null if the user signals end-of-input
   * (Ctrl-D) or interrupts (Ctrl-C).
   */
  fun readLine(prompt: String): String? = try {
    reader.readLine(prompt)
  } catch (_: UserInterruptException) {
    null
  } catch (_: EndOfFileException) {
    null
  }

  private fun buildReader(): LineReader {
    val jlineParser = DefaultParser().apply { escapeChars = charArrayOf() }
    val reader = LineReaderBuilder.builder()
      .terminal(terminal)
      .parser(jlineParser)
      .option(LineReader.Option.AUTO_LIST, false)
      .option(LineReader.Option.LIST_AMBIGUOUS, true)
      .build()

    var lastWasTab = false
    reader.widgets[WIDGET_KEY] = Widget {
      if (reader.lastBinding != "\t") {
        lastWasTab = false
      }
      val prefix = reader.buffer.upToCursor().takeLastWhile { !it.isWhitespace() }
      if (prefix.isEmpty()) {
        ringBell()
        lastWasTab = false
        return@Widget true
      }

      val matches = trie.getWordsWithPrefix(prefix).sorted()
      val longestCommonPrefix = trie.getLongestCommonPrefix(prefix)

      when {
        longestCommonPrefix != null -> {
          reader.buffer.write(longestCommonPrefix)
        }
        matches.size == 1 -> {
          reader.buffer.write(matches.first().removePrefix(prefix) + " ")
        }
        !lastWasTab -> {
          ringBell()
          lastWasTab = true
        }
        else -> {
          lastWasTab = false
          if (matches.isEmpty()) {
            ringBell()
          } else {
            printBelow(matches.joinToString("  "))
          }
        }
      }
      true
    }
    reader.keyMaps["main"]!!.bind(Reference(WIDGET_KEY), "\t")
    return reader
  }

  private fun ringBell() {
    terminal.writer().write(BELL)
    terminal.writer().flush()
  }

  private fun printBelow(text: String) {
    val writer = terminal.writer()
    writer.println()
    writer.println(text)
    writer.flush()
    reader.callWidget(LineReader.REDRAW_LINE)
    reader.callWidget(LineReader.REDISPLAY)
  }

  companion object {
    private const val BELL = ""
    private const val WIDGET_KEY = "path-complete"
  }
}