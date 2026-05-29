import command.Command
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintStream

data class Captured(val stdout: String, val stderr: String, val exitCode: Int)

fun Command.runCaptured(name: String, args: List<String>): Captured {
  val out = ByteArrayOutputStream()
  val err = ByteArrayOutputStream()
  val exit = execute(name, args, InputStream.nullInputStream(), PrintStream(out), PrintStream(err))
  return Captured(
    out.toString().trimEnd('\n'),
    err.toString().trimEnd('\n'),
    exit,
  )
}
