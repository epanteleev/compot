package driver

import common.pwd
import gen.IRGen
import preprocess.*
import ir.module.Module
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import tokenizer.CTokenizer
import parser.CProgramParser
import startup.*


class ShlangDriver(private val cli: ShlangCLIArguments) {

    private fun definedMacros(ctx: PreprocessorContext) {
        if (cli.getDefines().isEmpty()) {
            return
        }
        for ((name, value) in cli.getDefines()) {
            val tokens = CTokenizer.apply(value)
            val replacement = MacroReplacement(name, tokens)
            ctx.define(replacement)
        }
    }

    private fun initializePreprocessorContext(): PreprocessorContext {
        val pwd = pwd()
        val headerHolder = FileHeaderHolder(pwd, cli.getIncludeDirectories() + SYSTEM_HEADERS_PATH)

        val ctx = PreprocessorContext.empty(headerHolder)
        definedMacros(ctx)
        return ctx
    }

    private fun compile(): Module {
        val source = FileSystem.SYSTEM.read(cli.getFilename().toPath()) {
            readUtf8()
        }
        val ctx = initializePreprocessorContext()

        val tokens              = CTokenizer.apply(source, cli.getFilename())
        val preprocessor        = CProgramPreprocessor.create(tokens, ctx)
        val postProcessedTokens = preprocessor.preprocess()

        val parser     = CProgramParser.build(postProcessedTokens)
        val program    = parser.translation_unit()
        val typeHolder = parser.typeHolder()
        return IRGen.apply(typeHolder, program)
    }

    fun run() {
        val module = compile()
        OptDriver(cli.makeOptCLIArguments()).compile(module)
    }

    companion object {
        const val SYSTEM_HEADERS_PATH = "/usr/include"
    }
}