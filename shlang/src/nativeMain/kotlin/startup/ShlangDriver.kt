package startup

import gen.IRGen
import preprocess.*
import ir.module.Module
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path.Companion.toPath
import tokenizer.CTokenizer
import parser.CProgramParser
import platform.posix.*


class ShlangDriver(private val cli: CCLIArguments) {
    @OptIn(ExperimentalForeignApi::class)
    private fun initializePreprocessorContext(): PreprocessorContext {
        val pwd = getcwd(null, 0U)!!.toKString()
        val headerHolder = FileHeaderHolder(pwd, cli.getIncludeDirectories())
        return PreprocessorContext.empty(headerHolder)
    }

    private fun compile(): Module {
        val source = FileSystem.SYSTEM.read(cli.getFilename().toPath()) {
            readUtf8()
        }
        val ctx = initializePreprocessorContext()

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