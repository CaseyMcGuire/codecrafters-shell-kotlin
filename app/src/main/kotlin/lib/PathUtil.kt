package lib

import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

class PathUtil(
  private val pathProvider: () -> String = { System.getenv("PATH").orEmpty() },
) {

  val executablesOnPath: List<String> by lazy {
    pathProvider().split(File.pathSeparator)
      .asSequence()
      .map { Path(it) }
      .flatMap {
        runCatching { it.listDirectoryEntries() }.getOrDefault(emptyList())
      }
      .filter { it.isRegularFile() && it.isExecutable() }
      .map { it.fileName.toString() }
      .distinct()
      .toList()
  }

  fun getExecutablePath(commandText: String): String? =
    pathProvider().split(File.pathSeparator)
      .map { "$it/$commandText" }
      .firstOrNull {
        val file = File(it)
        file.isFile && file.canExecute()
      }

  fun isValidPath(path: String): Boolean {
    return File(path).exists()
  }
}