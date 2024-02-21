package startup

import java.io.File
import ir.module.Module
import java.nio.file.Paths
import ir.pass.PassPipeline
import ir.pass.CompileContextBuilder
import ir.platform.x64.codegen.x64CodeGenerator
import ir.read.ModuleReader


object Driver {
    private fun unoptimized(filename: String, module: Module) {
        val ctx = CompileContextBuilder(filename)
            .setSuffix(".base")
            .setLoggingLevel(1)
            .construct()

        val unoptimizedIr   = PassPipeline.base(ctx).run(module)
        val unoptimisedCode = x64CodeGenerator.emit(unoptimizedIr)

        val unoptimizedAsm = File("$filename/base.S")
        unoptimizedAsm.writeText(unoptimisedCode.toString())
    }

    private fun optimized(filename: String, module: Module) {
        val ctx = CompileContextBuilder(filename)
            .setSuffix(".opt")
            .setLoggingLevel(1)
            .construct()

        val destroyed        = PassPipeline.opt(ctx).run(module)
        val optimizedCodegen = x64CodeGenerator.emit(destroyed)

        val optimizedAsm = File("$filename/opt.S")
        optimizedAsm.writeText(optimizedCodegen.toString())
    }

    fun output(name: String, module: Module) {
        val filename = getName(name)

        val directoryName = File(filename)
        if (directoryName.exists()) {
            println("[Directory '$directoryName' exist. Remove...]")
            directoryName.deleteRecursively()
        }
        directoryName.mkdir()

        unoptimized(filename, module)
        optimized(filename, module)

        println("[Done '$filename']")
    }

    private fun getName(name: String): String {
        val fileName = Paths.get(name).fileName.toString()
        val lastIndex = fileName.lastIndexOf('.')
        if (lastIndex != -1) {
            return fileName.substring(0, lastIndex)
        }

        return fileName
    }

    fun run(args: Array<String>) {
        if (args.isEmpty()) {
            println("<ir-file>.ir")
            return
        }

        val text = File(args[0]).readText()
        val module = ModuleReader(text).read()

        output(args[0], module)
    }
}