package startup

import java.io.File
import ir.module.Module
import ir.pass.PassPipeline
import ir.read.ModuleReader
import ir.pass.CompileContextBuilder
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.Target
import java.nio.file.Files
import java.nio.file.StandardCopyOption


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
            .setTarget(Target.X64)

        val unoptimisedCode = codeGenerationFactory.build(unoptimizedIr)

        val temp           = Files.createTempFile("base", ".S")
        val unoptimizedAsm = File(temp.toString())
        unoptimizedAsm.writeText(unoptimisedCode.toString())
        GNUAssemblerRunner.run(unoptimizedAsm.toString(), "${commandLineArguments.getOutputFilename()}.o")

        if (commandLineArguments.isDumpIr()) {
            Files.copy(temp, File("${commandLineArguments.getDumpIrDirectory()}/${commandLineArguments.getBasename()}/base.S").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        unoptimizedAsm.delete()
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
            .setTarget(Target.X64)

        val optimizedCodegen = codeGenerationFactory.build(destroyed)

        val temp         = Files.createTempFile("opt", ".S")
        val optimizedAsm = temp.toFile()

        optimizedAsm.writeText(optimizedCodegen.toString())
        GNUAssemblerRunner.run(optimizedAsm.toString(), "${commandLineArguments.getOutputFilename()}.o")

        if (commandLineArguments.isDumpIr()) {
            Files.copy(temp, File("${commandLineArguments.getDumpIrDirectory()}/${commandLineArguments.getBasename()}/opt.S").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        optimizedAsm.delete()
    }

    private fun removeOrCreateDir() {
        if (!commandLineArguments.isDumpIr()) {
            return
        }
        val directoryName = File("${commandLineArguments.getDumpIrDirectory()}/${commandLineArguments.getBasename()}/")
        if (!directoryName.exists()) {
            directoryName.mkdirs()
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
        val text = File(commandLineArguments.getFilename()).readText()
        val module = ModuleReader(text).read()
        run(module)
    }
}