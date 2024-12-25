package startup

import common.ExecutionResult
import common.GNUAssemblerRunner
import ir.module.Module
import ir.pass.CompileContext
import ir.pass.PassPipeline
import ir.read.ModuleReader
import ir.pass.CompileContextBuilder
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.CompiledModule
import ir.platform.common.TargetPlatform
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.random.Random


class OptDriver(private val commandLineArguments: OptCLIArguments) {
    private fun runCompiler(suffix: String, asmFile: String, module: Module, pipeline: (CompileContext) -> PassPipeline): ExecutionResult {
        val builder = CompileContextBuilder(commandLineArguments.getBasename())
            .setSuffix(suffix)

        if (commandLineArguments.isDumpIr()) {
            builder.withDumpIr(commandLineArguments.getDumpIrDirectory())
        }

        val ctx                   = builder.construct()
        val unoptimizedIr         = pipeline(ctx).run(module)
        val codeGenerationFactory = CodeGenerationFactory()
            .setContext(ctx)
            .setTarget(TargetPlatform.X64)

        val unoptimisedCode = codeGenerationFactory.build(unoptimizedIr)
        return writeAsmFile(unoptimisedCode, asmFile)
    }

    private fun writeAsmFile(compiledModule: CompiledModule, asmFileName: String): ExecutionResult {
        val temp = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve(OPT + Random.nextInt())
        val optimizedAsm = temp.toString()
        try {
            FileSystem.SYSTEM.write(temp) {
                writeUtf8(compiledModule.toString())
            }

            if (commandLineArguments.isDumpIr()) {
                val dst = "${commandLineArguments.getDumpIrDirectory()}/${commandLineArguments.getBasename()}/$asmFileName".toPath()
                FileSystem.SYSTEM.copy(temp, dst)
            }

            return GNUAssemblerRunner.compileAsm(optimizedAsm, commandLineArguments.getOutputFilename())
        } finally {
            FileSystem.SYSTEM.delete(optimizedAsm.toPath())
        }
    }

    private fun removeOrCreateDir() {
        if (!commandLineArguments.isDumpIr()) {
            return
        }
        val directoryName = "${commandLineArguments.getDumpIrDirectory()}/${commandLineArguments.getBasename()}/".toPath()

        if (!FileSystem.SYSTEM.exists(directoryName)) {
            FileSystem.SYSTEM.createDirectories(directoryName)
        }
    }

    fun compile(module: Module) {
        removeOrCreateDir()
        val result = if (commandLineArguments.getOptLevel() == 0) {
            runCompiler(".base", BASE, module, PassPipeline::base)
        } else if (commandLineArguments.getOptLevel() == 1) {
            runCompiler(".opt", OPT, module, PassPipeline::opt)
        } else {
            println("Invalid optimization level: ${commandLineArguments.getOptLevel()}")
            return
        }

        if (result.exitCode != 0) {
            println("Error: ${result.error}")
        }
    }

    fun compile() {
        val text = FileSystem.SYSTEM.read(commandLineArguments.getFilename().toPath()) {
            readUtf8()
        }

        val module = try {
            ModuleReader(text).read()
        } catch (e: Exception) {
            println("Error: ${e.message}")
            throw e
        }

        compile(module)
    }

    companion object {
        const val BASE = "base.S"
        const val OPT = "opt.S"
    }
}