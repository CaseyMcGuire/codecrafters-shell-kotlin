package lib

import command.OutputDirection
import command.ParsedLine

class Parser {
  fun parse(line: String): ParsedLine {
    val tokens = mutableListOf<String>()
    var currentToken = StringBuilder()
    var parseState = ParseState.NONE
    var isEscaped = false
    for (char in line) {
      if (isEscaped) {
        if (parseState == ParseState.OPEN_DOUBLE_QUOTE) {
          if (char in setOf('$', '\\', '`', '"')) {
            currentToken.append(char)
          } else {
            currentToken.append('\\').append(char)
          }
        } else {
          currentToken.append(char)
        }
        isEscaped = false
        continue
      }
      when (char) {
        '\\' -> if (parseState != ParseState.OPEN_SINGLE_QUOTE) isEscaped = true
                else currentToken.append(char)
        ' ' -> {
          if (parseState == ParseState.NONE) {
            if (currentToken.isNotEmpty()) {
              tokens.add(currentToken.toString())
              currentToken = StringBuilder()
            }
          } else {
            currentToken.append(char)
          }
        }
        '\'' -> when (parseState) {
          ParseState.NONE -> parseState = ParseState.OPEN_SINGLE_QUOTE
          ParseState.OPEN_SINGLE_QUOTE -> parseState = ParseState.NONE
          else -> currentToken.append(char)
        }
        '"' -> when (parseState) {
          ParseState.NONE -> parseState = ParseState.OPEN_DOUBLE_QUOTE
          ParseState.OPEN_DOUBLE_QUOTE -> parseState = ParseState.NONE
          else -> currentToken.append(char)
        }
        else -> currentToken.append(char)
      }
    }
    if (currentToken.isNotEmpty()) {
      tokens.add(currentToken.toString())
    }
    val name = tokens.firstOrNull().orEmpty()
    return when {
      tokens.getOrNull(tokens.size - 2) in setOf(">", "1>", "1>>", ">>") -> ParsedLine(
        name,
        tokens.drop(1).dropLast(2),
        OutputDirection.File(tokens.last(), tokens[tokens.size - 2] in setOf("1>>", ">>")),
        OutputDirection.Print,
      )
      tokens.getOrNull(tokens.size - 2) in setOf("2>", "2>>") -> ParsedLine(
        name,
        tokens.drop(1).dropLast(2),
        OutputDirection.Print,
        OutputDirection.File(tokens.last(), tokens[tokens.size - 2] == "2>>"),
      )
      else -> ParsedLine(
        name,
        tokens.drop(1),
        OutputDirection.Print,
        OutputDirection.Print,
      )
    }
  }

  private enum class ParseState {
    NONE,
    OPEN_DOUBLE_QUOTE,
    OPEN_SINGLE_QUOTE,
  }
}
