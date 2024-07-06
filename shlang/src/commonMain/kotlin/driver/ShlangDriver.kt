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
import tokenizer.TokenList
import tokenizer.TokenPrinter


class ShlangDriver(private val cli: ShlangCLIArguments) {
    private fun definedMacros(ctx: PreprocessorContext) {
        if (cli.getDefines().isEmpty()) {
            return
        }
        for ((name, value) in cli.getDefines()) {
            val tokens      = CTokenizer.apply(value)
            val replacement = MacroReplacement(name, tokens)
            ctx.define(replacement)
        }
    }

    private fun initializePreprocessorContext(): PreprocessorContext {
        val pwd = pwd()
        val includeDirectories = cli.getIncludeDirectories() + SYSTEM_HEADERS_PATH + SYSTEM_LINUX_HEADERS_PATH + C_HEADERS_PATH
        val headerHolder       = FileHeaderHolder(pwd, includeDirectories)

        val ctx = PreprocessorContext.empty(headerHolder)
        definedMacros(ctx)
        return ctx
    }

    private fun preprocess(): TokenList? {
        val filename = cli.getFilename()
        val source = FileSystem.SYSTEM.read(filename.toPath()) {
            readUtf8()
        }
        val ctx = initializePreprocessorContext()

        val tokens              = CTokenizer.apply(source, filename)
        val preprocessor        = CProgramPreprocessor.create(tokens, ctx)
        val postProcessedTokens = preprocessor.preprocess()

        if (cli.isPreprocessOnly()) {
            TokenPrinter.print(postProcessedTokens)
            return null
        } else {
            return postProcessedTokens
        }
    }

    private fun compile(): Module? {
        val postProcessedTokens = preprocess()?: return null

        val parser     = CProgramParser.build(postProcessedTokens)
        val program    = parser.translation_unit()
        val typeHolder = parser.typeHolder()
        return IRGen.apply(typeHolder, program)
    }

    fun run() {
        val module = compile() ?: return
        OptDriver(cli.makeOptCLIArguments()).compile(module)
    }

    companion object {
        const val SYSTEM_HEADERS_PATH       = "/usr/include"
        const val SYSTEM_LINUX_HEADERS_PATH = "/usr/include/linux"
        const val C_HEADERS_PATH            = "/usr/lib/gcc/x86_64-pc-linux-gnu/14.1.1/include/" //TODO hardcoded for manjaro
    }
}