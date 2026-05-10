package lib

import java.io.File
import java.nio.file.Path

class PathUtil(
  private val pathProvider: () -> String = { System.getenv("PATH").orEmpty() },
) {
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

  fun getAdjustedPath(path: String): String {
    val pathComponents = getPathComponents(path)
    val adjustedPath = mutableListOf<String>()
    for (component in pathComponents) {
      when (component) {
        ".." -> adjustedPath.removeLastOrNull()
        "." -> { /* do nothing */ }
        else -> adjustedPath += component
      }
    }
    if (adjustedPath.isEmpty()) {
      return "/"
    }
    val newPath = Path.of(adjustedPath.first(), *adjustedPath.drop(1).toTypedArray()).toString()
    return if (Path.of(path).isAbsolute) {
      "/$newPath"
    } else {
      newPath
    }
  }


  fun getPathComponents(path: String): List<String> =
    Path.of(path)
        .map { it.toString() }

}