fun main() {
  while (true) {
    print("$ ")
    val line = readln()
    val (command, name, args) = Command.parse(line)
    command.execute(name, args)?.let(::println)
  }
}
