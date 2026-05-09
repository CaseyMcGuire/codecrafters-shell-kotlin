fun main() {
  while (true) {
    print("$ ")
    when (val line = readlnOrNull()) {
      "exit" -> return
      else -> println("$line: command not found")
    }
  }
}
