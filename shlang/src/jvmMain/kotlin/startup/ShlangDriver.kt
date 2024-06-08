package startup

import gen.IRGen
import preprocess.*
import java.io.File
import ir.module.Module
import tokenizer.CTokenizer
import parser.CProgramParser


class ShlangDriver(private val cli: CCLIArguments) {

    private fun initializePreprocessorContext(): PreprocessorContext {
        val pwd = System.getProperty("user.dir")
        val headerHolder = FileHeaderHolder(pwd, cli.getIncludeDirectories())
        return PreprocessorContext.empty(headerHolder)
    }

    private fun compile(): Module {
        val source = File(cli.getFilename()).readText()
        val ctx    = initializePreprocessorContext()

        val tokens              = CTokenizer.apply(source)
        val preprocessor        = CProgramPreprocessor.create(tokens, ctx)
        val postProcessedTokens = preprocessor.preprocessWithKilledSpaces()

        val program = CProgramParser.build(postProcessedTokens).translation_unit()
        return IRGen.apply(program)
    }

    fun run() {
        val module = compile()
        OptDriver(cli.makeOptCLIArguments()).run(module)
    }
}