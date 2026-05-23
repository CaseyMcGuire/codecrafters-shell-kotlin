package lib

import java.nio.file.Path

class CustomCompletionStore {

  val entries = mutableMapOf<String, Path>()

  fun add(path: Path, alias: String) {
    entries[alias] = path
  }
}

