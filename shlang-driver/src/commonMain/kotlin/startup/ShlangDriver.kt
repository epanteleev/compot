package startup

import codegen.GenerateIR
import common.pwd
import common.Extension
import common.Files
import common.GNULdRunner
import common.ProcessedFile
import preprocess.*
import ir.module.Module
import okio.FileSystem
import okio.Path.Companion.toPath
import tokenizer.CTokenizer
import parser.CProgramParser
import preprocess.macros.MacroReplacement
import tokenizer.TokenList
import tokenizer.TokenPrinter
import kotlin.collections.iterator


class ShlangDriver(private val cli: ShlangArguments) {
    private fun definedMacros(ctx: PreprocessorContext) {
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

        val ctx = PreprocessorContext.create(headerHolder)
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

    private fun makeOptCLIArguments(inputFilename: ProcessedFile): OptCLIArguments {
        val optCLIArguments = OptCLIArguments()
        optCLIArguments.setFilename(inputFilename.withExtension(Extension.IR))
            .setOptLevel(cli.getOptLevel())
            .setDumpIrDirectory(cli.getDumpIrDirectory())

        val outFilename = cli.getOutputFilename()
        if (cli.isCompile() && outFilename == ShlangArguments.DEFAULT_OUTPUT) {
            optCLIArguments.setOutputFilename(inputFilename.withExtension(Extension.OBJ))
        } else {
            optCLIArguments.setOutputFilename(outFilename.withExtension(Extension.OBJ))
        }

        return optCLIArguments
    }

    private fun compile(filename: String): Module? {
        val postProcessedTokens = preprocess(filename)?: return null

        val parser     = CProgramParser.build(filename, postProcessedTokens)
        val program    = parser.translation_unit()
        val typeHolder = parser.typeHolder()
        return GenerateIR.apply(typeHolder, program)
    }

    private fun runLD(out: ProcessedFile, compiledFiles: List<ProcessedFile>, crtObjs: List<String>) {
        val result = GNULdRunner(out)
            .libs(SystemConfig.runtimeLibraries() + cli.getDynamicLibraries())
            .libPaths(SystemConfig.runtimePathes())
            .crtObjects(crtObjs)
            .objs(compiledFiles.map { it.filename })
            .dynamicLinker(SystemConfig.dynamicLinker())
            .execute()

        if (result.exitCode != 0) {
            println("Error: ${result.error}")
        }
    }

    fun run() { //TODo: move some actions to separate class LDDriver
        val processedFiles = arrayListOf<ProcessedFile>()
        for (input in cli.inputs()) {
            when (input.extension) {
                Extension.AR -> processedFiles.add(input)
                Extension.OBJ -> processedFiles.add(input)
                Extension.C -> {
                    val module = compile(input.filename) ?: continue
                    val cli = makeOptCLIArguments(input)
                    val objFile = OptDriver.compile(cli, module)
                    processedFiles.add(objFile)
                }
                else -> throw IllegalStateException("Invalid input file: $input")
            }
        }

        if (cli.isCompile()) {
            return
        }

        val out = cli.getOutputFilename()
        when (out.extension) {
            Extension.EXE -> {
                if (cli.isSharedOption()) {
                    throw IllegalStateException("Cannot create executable for shared object")
                }
                runLD(out, processedFiles, SystemConfig.crtStaticObjects())
            }
            Extension.SO -> {
                if (!cli.isSharedOption()) {
                    throw IllegalStateException("Cannot create shared object for executable")
                }

                runLD(out, processedFiles, SystemConfig.crtSharedObjects())
            }
            else -> throw IllegalStateException("Invalid output file extension: $out")
        }
    }
}