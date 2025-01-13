package startup

import common.pwd
import codegen.IRGen
import common.Files
import common.GNULdRunner
import preprocess.*
import ir.module.Module
import okio.FileSystem
import okio.Path.Companion.toPath
import tokenizer.CTokenizer
import parser.CProgramParser
import preprocess.macros.MacroReplacement
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

    private fun initializePreprocessorContext(filename: String): PreprocessorContext {
        val usrDir = SystemConfig.systemHeadersPaths() ?: throw IllegalStateException("Cannot find system include directory")
        val includeDirectories = cli.getIncludeDirectories() + usrDir
        val workingDirectory   = Files.getDirName(filename)
        val headerHolder       = FileHeaderHolder(pwd(), includeDirectories + workingDirectory)

        val ctx = PreprocessorContext.empty(headerHolder)
        definedMacros(ctx)
        return ctx
    }

    private fun preprocess(filename: String): TokenList? {
        val source = FileSystem.SYSTEM.read(filename.toPath()) {
            readUtf8()
        }
        val ctx = initializePreprocessorContext(filename)

        val tokens              = CTokenizer.apply(source, filename)
        val preprocessor        = CProgramPreprocessor.create(filename, tokens, ctx)
        val postProcessedTokens = preprocessor.preprocess()

        if (cli.isPreprocessOnly() && cli.isDumpDefines()) {
            for (token in ctx.macroReplacements()) {
                println(token.value.tokenString())
            }
            for (token in ctx.macroDefinitions()) {
                println(token.value.tokenString())
            }
            for (token in ctx.macroFunctions()) {
                println(token.value.tokenString())
            }
            return null
        } else if (cli.isPreprocessOnly()) {
            println(TokenPrinter.print(postProcessedTokens))
            return null
        } else {
            return postProcessedTokens
        }
    }

    private fun makeOptCLIArguments(inputFilename: String): OptCLIArguments {
        val optCLIArguments = OptCLIArguments()
        optCLIArguments.setFilename(inputFilename)
        optCLIArguments.setOptLevel(cli.getOptLevel())

        val dumpIrDirectoryOutput = cli.getDumpIrDirectory()
        if (dumpIrDirectoryOutput != null) {
            optCLIArguments.setDumpIrDirectory(dumpIrDirectoryOutput)
        }

        val outFilename = cli.getOutputFilename()
        optCLIArguments.setOutputFilename(outFilename.filename)
        return optCLIArguments
    }

    private fun compile(filename: String): Module? {
        val postProcessedTokens = preprocess(filename)?: return null

        val parser     = CProgramParser.build(filename, postProcessedTokens)
        val program    = parser.translation_unit()
        val typeHolder = parser.typeHolder()
        return IRGen.apply(typeHolder, program)
    }

    fun run() {
        for (input in cli.inputs()) {
            val module = compile(input.filename) ?: continue
            OptDriver(makeOptCLIArguments(input.filename)).compile(module)
        }

        val out = cli.getOutputFilename()
        if (out.extension != Extension.EXE) {
            return
        }

        val objModules = SystemConfig.crtObjects() ?: throw IllegalStateException("Cannot find crt objects")
        val result = GNULdRunner("a.out")
            .libs(SystemConfig.runtimeLibraries())
            .objs(objModules + out.filename)
            .dynamicLinker(SystemConfig.dynamicLinker())
            .execute()

        if (result.exitCode != 0) {
            println("Error: ${result.error}")
        }
    }
}