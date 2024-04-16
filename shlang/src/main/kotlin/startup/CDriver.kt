package startup

import gen.IRGen
import parser.ProgramParser
import tokenizer.CTokenizer
import java.io.File

class CDriver(private val cli: CCLIArguments) {
    fun run() {
        val source = File(cli.getFilename()).readText()
        val tokens = CTokenizer.apply(source)
        val program = ProgramParser(tokens).program()
        val module = IRGen.apply(program)
        OptDriver(cli.makeOptCLIArguments()).run(module)
    }
}