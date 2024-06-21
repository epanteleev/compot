package startup

import common.GNUAssemblerRunner
import ir.module.Module
import ir.pass.PassPipeline
import ir.read.ModuleReader
import ir.pass.CompileContextBuilder
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.CompiledModule
import ir.platform.common.TargetPlatform
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM


class OptDriver(private val commandLineArguments: OptCLIArguments) {
    private fun unoptimized(module: Module) {
        val builder = CompileContextBuilder(commandLineArguments.getBasename())
            .setSuffix(".base")

        if (commandLineArguments.isDumpIr()) {
            builder.withDumpIr(commandLineArguments.getDumpIrDirectory())
        }

        val ctx                   = builder.construct()
        val unoptimizedIr         = PassPipeline.base(ctx).run(module)
        val codeGenerationFactory = CodeGenerationFactory()
            .setContext(ctx)
            .setTarget(TargetPlatform.X64)

        val unoptimisedCode = codeGenerationFactory.build(unoptimizedIr)
        writeAsmFile(unoptimisedCode, BASE)
    }

    private fun optimized(module: Module) {
        val builder = CompileContextBuilder(commandLineArguments.getBasename())
            .setSuffix(".opt")

        if (commandLineArguments.isDumpIr()) {
            builder.withDumpIr(commandLineArguments.getDumpIrDirectory())
        }

        val ctx                   = builder.construct()
        val destroyed             = PassPipeline.opt(ctx).run(module)
        val codeGenerationFactory = CodeGenerationFactory()
            .setContext(ctx)
            .setTarget(TargetPlatform.X64)

        val optimizedCodegen = codeGenerationFactory.build(destroyed)
        writeAsmFile(optimizedCodegen, OPT)
    }

    private fun writeAsmFile(compiledModule: CompiledModule, asmFileName: String) {
        val temp = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve(OPT)
        val optimizedAsm = temp.toString()
        try {
            FileSystem.SYSTEM.write(temp) {
                writeUtf8(compiledModule.toString())
            }

            if (commandLineArguments.isDumpIr()) {
                val dst = "${commandLineArguments.getDumpIrDirectory()}/${commandLineArguments.getBasename()}/$asmFileName".toPath()
                FileSystem.SYSTEM.copy(temp, dst)
            }
            GNUAssemblerRunner.run(optimizedAsm, "${commandLineArguments.getOutputFilename()}.o")
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

    fun run(module: Module) {
        removeOrCreateDir()
        if (commandLineArguments.getOptLevel() == 0) {
            unoptimized(module)
        } else if (commandLineArguments.getOptLevel() == 1) {
            optimized(module)
        } else {
            println("Invalid optimization level: ${commandLineArguments.getOptLevel()}")
            return
        }
    }

    fun run() {
        val text = FileSystem.SYSTEM.read(commandLineArguments.getFilename().toPath()) {
            readUtf8()
        }

        val module = try {
            ModuleReader(text).read()
        } catch (e: Exception) {
            println("Error: ${e.message}")
            throw e
        }

        run(module)
    }

    companion object {
        const val BASE = "base.S"
        const val OPT = "opt.S"
    }
}