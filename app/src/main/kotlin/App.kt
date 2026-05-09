fun main() {
  while (true) {
    print("$ ")
    val line = readln()
    val lineSplit = line.split(" ")
                        .filter { it.isNotBlank() }
    when {
      lineSplit.firstOrNull() == "echo" -> println(lineSplit.drop(1).joinToString(" "))
      lineSplit.firstOrNull() == "exit" -> return
      else -> println("$line: command not found")
    }
  }
}
