import kotlin.test.Test
class ProbeTest {
  @Test
  fun probe() {
    val parsed = Shell(lib.PathUtil()).parse("echo hi > out.txt")
    println("DEBUG name=${parsed.name} args=${parsed.args} dir=${parsed.standardOutputDirection}")
    println("DEBUG dir.path=${(parsed.standardOutputDirection as? command.StandardOutputDirection.File)?.path}")
  }
}
