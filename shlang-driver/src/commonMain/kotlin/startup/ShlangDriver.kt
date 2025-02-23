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
import kotlin.random.Random


class ShlangDriver(private val cli: ShlangArguments) {
    private fun definedMacros(ctx: PreprocessorContext) {
        for ((name, value) in cli.getDefines()) {
            val tokens      = CTokenizer.apply(value, "<input>")
            val replacement = MacroReplacement(name, tokens)
            ctx.define(replacement)
        }
    }

    private fun initializePreprocessorContext(filename: String): PreprocessorContext {
        val usrDir = SystemConfig.systemHeadersPaths()
        val includeDirectories = cli.getIncludeDirectories() + usrDir
        val workingDirectory   = Files.getDirName(filename)
        val headerHolder       = FileHeaderHolder(pwd(), includeDirectories + workingDirectory)

        val ctx = PreprocessorContext.create(headerHolder)
        definedMacros(ctx)
        return ctx
    }

    private fun preprocess(filename: String): TokenList? {
        val source = FileSystem.SYSTEM.read(filename.toPath()) {
            readUtf8() //TODO: stream reading
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
        val file = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve(inputFilename.basename() + Random.nextInt() + ".o")
        val optCLIArguments = OptCLIArguments()
        optCLIArguments.setFilename(inputFilename.withExtension(Extension.IR))
            .setOptLevel(cli.getOptLevel())
            .setDumpIrDirectory(cli.getDumpIrDirectory())
            .setOutputFilename(ProcessedFile.fromFilename(file.toString()))
            .setPic(cli.pic())

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
        logDebug { "Linking files: $compiledFiles" }
        val result = GNULdRunner(out)
            .libs(SystemConfig.runtimeLibraries() + cli.getDynamicLibraries())
            .libPaths(SystemConfig.runtimePathes())
            .crtObjects(crtObjs)
            .objs(compiledFiles)
            .dynamicLinker(SystemConfig.dynamicLinker())
            .execute()

        if (result.exitCode != 0) {
            println("Error: ${result.error}")
        }
    }

    private fun compileCFile(input: ProcessedFile): ProcessedFile? {
        logDebug {
            "Compiling file: $input"
        }

        val module = compile(input.filename) ?: return null
        val cli = makeOptCLIArguments(input)
        val objFile = OptDriver.compile(cli, module)
        logDebug {
            "Compiled file: $objFile"
        }
        return objFile
    }

    fun run() { //TODo: move some actions to separate class LDDriver
        val processedFiles = arrayListOf<ProcessedFile>()
        val compiled = arrayListOf<ProcessedFile>()
        for (input in cli.inputs()) {
            when (input.extension) {
                Extension.AR -> processedFiles.add(input)
                Extension.OBJ -> processedFiles.add(input)
                Extension.C -> {
                    val objFile = compileCFile(input) ?: continue
                    compiled.add(objFile)
                }
                else -> throw IllegalStateException("Invalid input file: $input")
            }
        }

        if (cli.isCompile()) {
            val output = cli.getOutputFilename()
            if (output != ShlangArguments.DEFAULT_OUTPUT) {
                val src = compiled.first()
                logDebug {
                    "Copying file: $src to $output"
                }
                FileSystem.SYSTEM.copy(src.filename.toPath(), output.filename.toPath())
                return
            }

            for (i in cli.inputs().indices) {
                val input = cli.inputs()[i]
                val compiledFile = compiled[i]
                if (input.extension != Extension.C) {
                    continue
                }

                val dst = input.withExtension(Extension.OBJ)
                val src = compiledFile.filename
                logDebug {
                    "Copying file: $src to $dst"
                }
                FileSystem.SYSTEM.copy(src.toPath(), dst.filename.toPath())
            }

            return
        }

        val out = cli.getOutputFilename()
        when (out.extension) {
            Extension.EXE -> {
                if (cli.isSharedOption()) {
                    throw IllegalStateException("Cannot create executable for shared object")
                }

                runLD(out, compiled + processedFiles, SystemConfig.crtStaticObjects())
            }
            Extension.SO -> {
                if (!cli.isSharedOption()) {
                    throw IllegalStateException("Cannot create shared object for executable")
                }

                runLD(out, compiled + processedFiles, SystemConfig.crtSharedObjects())
            }
            else -> throw IllegalStateException("Invalid output file extension: $out")
        }
    }

    fun logDebug(message: () -> String) {
        cli.logger().debug(message)
    }
}