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
import tokenizer.Position
import tokenizer.toAnyToken


class ShlangDriver(private val cli: ShlangCLIArguments) {

    private fun definedMacros(ctx: PreprocessorContext) {
        if (cli.getDefines().isEmpty()) {
            return
        }
        for ((name, value) in cli.getDefines()) {
            val replacement = MacroReplacement(name, listOf(value.toAnyToken(Position.UNKNOWN)))
            ctx.define(replacement)
        }
    }

    private fun initializePreprocessorContext(): PreprocessorContext {
        val pwd = pwd()
        val headerHolder = FileHeaderHolder(pwd, cli.getIncludeDirectories())

        val ctx = PreprocessorContext.empty(headerHolder)
        definedMacros(ctx)
        return ctx
    }

    private fun compile(): Module {
        val source = FileSystem.SYSTEM.read(cli.getFilename().toPath()) {
            readUtf8()
        }
        val ctx = initializePreprocessorContext()

        val tokens              = CTokenizer.apply(source)
        val preprocessor        = CProgramPreprocessor.create(tokens, ctx)
        val postProcessedTokens = preprocessor.preprocessWithKilledSpaces()

        val parser     = CProgramParser.build(postProcessedTokens)
        val program    = parser.translation_unit()
        val typeHolder = parser.typeHolder()
        return IRGen.apply(typeHolder, program)
    }

    fun run() {
        val module = compile()
        OptDriver(cli.makeOptCLIArguments()).compile(module)
    }
}