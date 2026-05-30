package lib

import ShellState
import command.OutputDirection
import command.ParsedCommand

class Parser(private val shellState: ShellState) {
  fun parse(line: String): List<ParsedCommand> {
    val segments = mutableListOf<List<String>>()
    var tokens = mutableListOf<String>()
    var currentToken = StringBuilder()
    var parseState = ParseState.NONE
    var isEscaped = false

    fun flushToken() {
      if (currentToken.isNotEmpty()) {
        tokens.add(currentToken.toString())
        currentToken = StringBuilder()
      }
    }

    fun flushSegment() {
      flushToken()
      segments.add(tokens)
      tokens = mutableListOf()
    }

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
            flushToken()
          } else {
            currentToken.append(char)
          }
        }
        '|' -> {
          if (parseState == ParseState.NONE) {
            flushSegment()
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

    flushSegment()

    return segments.map(::toParsedCommand)
  }

  private fun toParsedCommand(tokens: List<String>): ParsedCommand {
    val name = tokens.firstOrNull().orEmpty()

    val (commandName, args, stdOut, stdErr) = when {
      tokens.getOrNull(tokens.size - 2) in setOf(">", "1>", "1>>", ">>") -> ParsedCommand(
        name,
        tokens.drop(1).dropLast(2),
        OutputDirection.File(tokens.last(), tokens[tokens.size - 2] in setOf("1>>", ">>")),
        OutputDirection.Print,
      )
      tokens.getOrNull(tokens.size - 2) in setOf("2>", "2>>") -> ParsedCommand(
        name,
        tokens.drop(1).dropLast(2),
        OutputDirection.Print,
        OutputDirection.File(tokens.last(), tokens[tokens.size - 2] == "2>>"),
      )
      else -> ParsedCommand(
        name,
        tokens.drop(1),
        OutputDirection.Print,
        OutputDirection.Print,
      )
    }

    return ParsedCommand(
      commandName,
      args.map(::replaceVariables),
      stdOut,
      stdErr,
    )
  }

  private fun replaceVariables(str: String): String {
    val builder = StringBuilder()
    var variableBuilder = StringBuilder()
    var inVariable = false

    val append = { c: Char ->
      if (inVariable) variableBuilder.append(c)
      else builder.append(c)
    }
    for (i in str.indices) {
      val c = str[i]
      if (c == '$') {
        inVariable = true
      }
      else if (c == '{') {
        if (inVariable) {
          continue
        }
        else {
          append(c)
        }
      }
      else if (c == '}') {
        val replacement = shellState.variables[variableBuilder.toString()]
        replacement?.let { builder.append(it) }
        variableBuilder = StringBuilder()
        inVariable = false
      }
      else {
        append(c)
      }

    }

    if (variableBuilder.isNotEmpty()) {
      val replacement = shellState.variables[variableBuilder.toString()]
      replacement?.let { builder.append(it) }
    }

    return builder.toString()
  }

  private enum class ParseState {
    NONE,
    OPEN_DOUBLE_QUOTE,
    OPEN_SINGLE_QUOTE,
  }
}