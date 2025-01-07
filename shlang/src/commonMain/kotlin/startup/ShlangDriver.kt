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
        val includeDirectories = cli.getIncludeDirectories() + USR_INCLUDE_PATH + USR_INCLUDE_GNU_LINUX_PATH
        val workingDirectory   = Files.getDirName(filename)
        val headerHolder       = FileHeaderHolder(pwd(), includeDirectories + workingDirectory)

        val ctx = PreprocessorContext.empty(headerHolder)
        definedMacros(ctx)
        return ctx
    }

    private fun preprocess(): TokenList? {
        val filename = cli.getFilename()
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

    private fun compile(): Module? {
        val postProcessedTokens = preprocess()?: return null

        val parser     = CProgramParser.build(cli.getFilename(), postProcessedTokens)
        val program    = parser.translation_unit()
        val typeHolder = parser.typeHolder()
        return IRGen.apply(typeHolder, program)
    }

    fun run() {
        val module = compile() ?: return
        OptDriver(cli.makeOptCLIArguments()).compile(module)

        if (cli.getOutputFilename().endsWith(".o")) {
            return
        }

        val result = GNULdRunner("a.out")
            .libs(libs)
            .objs(objModules + cli.getOutputFilename())
            .dynamicLinker(dynamicLinker)
            .execute()

        if (result.exitCode != 0) {
            println("Error: ${result.error}")
        }
    }

    companion object {
        const val USR_INCLUDE_PATH = "/usr/include" // Manjaro
        const val USR_INCLUDE_GNU_LINUX_PATH = "/usr/include/x86_64-linux-gnu" // Ubuntu

        private val libs = arrayListOf( // Manjaro
            "-L/usr/lib/x86_64-linux-gnu",
            "-L/usr/lib64",
            "-lc",
        )

        private val objModules = arrayListOf( // Manjaro
            "/usr/lib64/crti.o",
            "/usr/lib64/crt1.o",
            "/usr/lib64/crtn.o"
         )

        /*private val objModules = arrayListOf(
            "/usr/lib/x86_64-linux-gnu/crti.o",
            "/usr/lib/x86_64-linux-gnu/crt1.o",
            "/usr/lib/x86_64-linux-gnu/crtn.o"
        )*/

        private const val dynamicLinker = "/lib64/ld-linux-x86-64.so.2"
    }
}