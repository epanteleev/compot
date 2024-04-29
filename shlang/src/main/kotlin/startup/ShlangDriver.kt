package startup

import gen.IRGen
import parser.CProgramParser
import tokenizer.CTokenizer
import java.io.File

class ShlangDriver(private val cli: CCLIArguments) {
    fun run() {
        val source = File(cli.getFilename()).readText()
        val tokens = CTokenizer.apply(source)
        val program = CProgramParser(tokens).translation_unit()
        val module = IRGen.apply(program)
        OptDriver(cli.makeOptCLIArguments()).run(module)
    }
}