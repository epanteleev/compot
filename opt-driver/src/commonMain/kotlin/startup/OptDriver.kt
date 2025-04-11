package startup

import common.*
import ir.module.Module
import ir.pass.CompileContext
import ir.pass.CompileContextBuilder
import ir.pass.PassPipeline
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.CompiledModule
import ir.platform.common.TargetPlatform
import java.io.PrintWriter
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.random.Random


class OptDriver private constructor(private val commandLineArguments: OptCLIArguments) {
    private fun inputBasename(): String {
        return commandLineArguments.inputs().first().basename()
    }

    private fun runCompiler(suffix: String, asmFile: String, module: Module, pipeline: (CompileContext) -> PassPipeline): ExecutionResult {
        val builder = CompileContextBuilder(inputBasename())
            .setSuffix(suffix)

        if (commandLineArguments.isDumpIr()) {
            builder.withDumpIr(commandLineArguments.getDumpIrDirectory())
        }

        val ctx                   = builder.construct()
        val unoptimizedIr         = pipeline(ctx).run(module)
        val codeGenerationFactory = CodeGenerationFactory()
            .setContext(ctx)
            .setTarget(TargetPlatform.X64)
            .pic(false ) // (commandLineArguments.isPic())

        val unoptimisedCode = codeGenerationFactory.build(unoptimizedIr)
        return compileAsmFile(unoptimisedCode, asmFile)
    }

    private fun compileAsmFile(compiledModule: CompiledModule, asmFileName: String): ExecutionResult {
        val tempDir = FileUtils.createTempFile(OPT + Random.nextInt())
        val optimizedAsm = tempDir.toString()
        try {
            PrintWriter(optimizedAsm, Charsets.UTF_8).use { out ->
                out.println(compiledModule.toString())
            }

            if (commandLineArguments.isDumpIr()) {
                val dst = "${commandLineArguments.getDumpIrDirectory()}/${inputBasename()}/$asmFileName"
                PrintWriter(dst, Charsets.UTF_8).use { out ->
                    out.println(compiledModule.toString())
                }
            }

            val output = commandLineArguments.getOutputFilename().withExtension(Extension.OBJ)
            return GNUAssemblerRunner.compileAsm(optimizedAsm, output.filename)
        } finally {
            tempDir.deleteIfExists()
        }
    }

    private fun removeOrCreateDir() {
        if (!commandLineArguments.isDumpIr()) {
            return
        }
        val directoryName = Path.of("${commandLineArguments.getDumpIrDirectory()}/${inputBasename()}/")

        if (!directoryName.exists()) {
            FileUtils.deleteDirectory(directoryName.toFile())
        }
    }

    private fun compile(module: Module): ProcessedFile {
        removeOrCreateDir()
        val result = if (commandLineArguments.getOptLevel() == 0) {
            runCompiler(".base", BASE, module, PassPipeline::base)
        } else if (commandLineArguments.getOptLevel() >= 1) {
            runCompiler(".opt", OPT, module, PassPipeline::opt)
        } else {
            throw IllegalArgumentException("Invalid optimization level: -O${commandLineArguments.getOptLevel()}")
        }

        if (result.exitCode != 0) {
            throw IllegalStateException("execution failed with code ${result.exitCode}:\n${result.error}")
        }

        return commandLineArguments.getOutputFilename().withExtension(Extension.OBJ)
    }

    companion object {
        private const val BASE = "base.S"
        private const val OPT = "opt.S"

        fun compile(cli: OptCLIArguments, module: Module): ProcessedFile {
            return OptDriver(cli).compile(module)
        }
    }
}