package launcher

import common.RunExecutable
import startup.CliParser
import startup.OptDriver
import kotlin.test.assertEquals


fun assertStdio(filename: String, lib: List<String>, expectedStdio: String) {
    val args = arrayOf("-c", "$filename.ir")

    val cli = CliParser().parse(args) ?: return
    OptDriver(cli).run()

    val gnuAsCommandLine = listOf("gcc", "$filename.o") + lib + listOf("-o", filename)
    val result = RunExecutable.runCommand(gnuAsCommandLine, null)
    assertEquals(expectedStdio, result.output)
}