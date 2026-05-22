package lib

import ShellState
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
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

class TerminalReader(
  completions: List<String>,
  private val terminal: Terminal = TerminalBuilder.builder().build(),
  private val shellState: ShellState,
  private val trie: Trie = Trie(completions.distinct()),
) {
  private val reader: LineReader = buildReader()
  private val editor: LineEditor = JLineEditor(reader, terminal)
  private var lastWasTab = false

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

  internal fun handleTab(editor: LineEditor) {
    if (!editor.wasLastBindingTab) {
      lastWasTab = false
    }

    val words = editor.textBeforeCursor.split(" ")
      .filter { it.isNotEmpty() }
    val prefix = words.lastOrNull() ?: ""
    if (prefix.isEmpty()) {
      editor.bell()
      lastWasTab = false
      return
    }

    val isArgPosition = words.size > 1 || (words.size == 1 && editor.textBeforeCursor.endsWith(" "))
    if (isArgPosition) {
      completeArgument(editor, words)
      return
    }

    val matches = trie.getWordsWithPrefix(prefix).sorted()
    val longestCommonPrefix = trie.getLongestCommonPrefix(prefix)

    when {
      matches.size == 1 -> {
        editor.insertAtCursor(matches.first().removePrefix(prefix) + " ")
      }
      longestCommonPrefix != null -> {
        editor.insertAtCursor(longestCommonPrefix)
      }
      !lastWasTab -> {
        editor.bell()
        lastWasTab = true
      }
      else -> {
        lastWasTab = false
        if (matches.isEmpty()) editor.bell()
        else editor.listBelow(matches.joinToString("  "))
      }
    }
  }

  /**
   * Complete a filename or directory name against the current working directory (or a
   * relative subdirectory). [words] is the whitespace-split prefix typed so far; if the
   * cursor is right after a trailing space (i.e. the last word has been "closed off"),
   * we fall back to listing dirs in cwd.
   */
  private fun completeArgument(editor: LineEditor, words: List<String>) {
    val cursorAtTrailingSpace = editor.textBeforeCursor.endsWith(" ")
    val (directory, fileNamePrefix) = when {
      cursorAtTrailingSpace -> Path(shellState.currentWorkingDirectory) to ""
      words.last().contains("/") -> Pair(
        Path("${shellState.currentWorkingDirectory}/${words.last().substringBeforeLast("/")}"),
        words.last().substringAfterLast("/"),
      )
      else -> Path(shellState.currentWorkingDirectory) to words.last()
    }

    val matches = directory.listDirectoryEntries()
      .filter { it.fileName.toString().startsWith(fileNamePrefix) }
      .let { if (cursorAtTrailingSpace) it.filter { entry -> entry.isDirectory() } else it }

    if (matches.isEmpty()) {
      editor.bell()
    }
    else if (matches.size == 1) {
      val match = matches.first()
      val suffix = match.fileName.toString().removePrefix(fileNamePrefix)
      val trailing = if (match.isRegularFile()) " " else "/"
      editor.insertAtCursor(suffix + trailing)
    }
    else {
      editor.listBelow(
        matches
          .map { if (it.isDirectory()) "${it}/" else it }
        .joinToString("  ")
      )
    }
  }

  private fun buildReader(): LineReader {
    val jlineParser = DefaultParser().apply { escapeChars = charArrayOf() }
    val reader = LineReaderBuilder.builder()
      .terminal(terminal)
      .parser(jlineParser)
      .option(LineReader.Option.AUTO_LIST, false)
      .option(LineReader.Option.LIST_AMBIGUOUS, true)
      .build()

    reader.widgets[WIDGET_KEY] = Widget {
      handleTab(editor)
      true
    }
    reader.keyMaps["main"]!!.bind(Reference(WIDGET_KEY), "\t")
    return reader
  }

  companion object {
    private const val WIDGET_KEY = "path-complete"
  }
}
