package startup

import java.io.File
import ir.module.Module
import ir.pass.PassPipeline
import ir.read.ModuleReader
import ir.pass.CompileContextBuilder
import ir.platform.CodeGenerationFactory
import ir.platform.Target
import java.nio.file.Files
import java.nio.file.StandardCopyOption


class Driver(private val commandLineArguments: CommandLineArguments) {
    private fun unoptimized(filename: String, module: Module) {
        val ctx = CompileContextBuilder(filename)
            .setSuffix(".base")
            .withDumpIr(commandLineArguments.isDumpIr())
            .construct()

        val unoptimizedIr = PassPipeline.base(ctx).run(module)

        val codeGenerationFactory = CodeGenerationFactory()
            .setContext(ctx)
            .setTarget(Target.X64)

        val unoptimisedCode = codeGenerationFactory.build(unoptimizedIr)

        val temp = Files.createTempFile("base", ".S")
        val unoptimizedAsm = File(temp.toString())
        unoptimizedAsm.writeText(unoptimisedCode.toString())
        AssemblerRunner.run(unoptimizedAsm.toString(), "$filename.o")

        if (commandLineArguments.isDumpIr()) {
            Files.copy(temp, File("$filename/base.S").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun optimized(filename: String, module: Module) {
        val ctx = CompileContextBuilder(filename)
            .setSuffix(".opt")
            .withDumpIr(commandLineArguments.isDumpIr())
            .construct()

        val destroyed = PassPipeline.opt(ctx).run(module)

        val codeGenerationFactory = CodeGenerationFactory()
            .setContext(ctx)
            .setTarget(Target.X64)

        val optimizedCodegen = codeGenerationFactory.build(destroyed)

        val temp = Files.createTempFile("opt", ".S")
        val optimizedAsm = File(temp.toString())

        optimizedAsm.writeText(optimizedCodegen.toString())
        AssemblerRunner.run(optimizedAsm.toString(), "$filename.o")

        if (commandLineArguments.isDumpIr()) {
            Files.copy(temp, File("$filename/opt.S").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun removeOrCreateDir() {
        if (!commandLineArguments.isDumpIr()) {
            return
        }
        val directoryName = File(commandLineArguments.getLogDir())
        if (!directoryName.exists()) {
            directoryName.mkdir()
        }
    }

    fun run() {
        val text = File(commandLineArguments.getFilename()).readText()
        removeOrCreateDir()
        val module = ModuleReader(text).read()

        val filename = commandLineArguments.getBasename()
        if (commandLineArguments.getOptLevel() == 0) {
            unoptimized(filename, module)
        } else if (commandLineArguments.getOptLevel() == 1) {
            optimized(filename, module)
        } else {
            println("Invalid optimization level: ${commandLineArguments.getOptLevel()}")
            return
        }
    }
}