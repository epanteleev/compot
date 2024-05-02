package startup

import gen.IRGen
import parser.CProgramParser
import preprocess.CProgramPreprocessor
import preprocess.FileHeaderHolder
import preprocess.HeaderHolder
import preprocess.PreprocessorContext
import tokenizer.CTokenizer
import java.io.File

class ShlangDriver(private val cli: CCLIArguments) {
    fun run() {
        val source = File(cli.getFilename()).readText()
        val ctx = PreprocessorContext.empty(FileHeaderHolder(setOf(File(cli.getFilename()).parent)))
        val tokens = CTokenizer.apply(source)
        val preprocessor = CProgramPreprocessor.create(tokens, ctx)
        val postProcessedTokens = preprocessor.preprocessWithKilledSpaces()

        val program = CProgramParser.build(postProcessedTokens).translation_unit()
        val module = IRGen.apply(program)
        OptDriver(cli.makeOptCLIArguments()).run(module)
    }
}