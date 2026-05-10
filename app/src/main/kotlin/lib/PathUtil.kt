package lib

import java.io.File

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
}