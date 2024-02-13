package startup

import ir.module.Module
import ir.pass.ana.VerifySSA
import ir.pass.transform.SSADestructionFabric
import ir.platform.x64.codegen.x64CodeGenerator
import java.io.File
import java.nio.file.Paths


object Driver {
    fun output(name: String, module: Module, pipeline: (Module) -> Module) {
        val filename         = getName(name)
        val unoptimizedIr    = VerifySSA.run(SSADestructionFabric.create(module).run())
        val unoptimisedCode  = x64CodeGenerator.emit(unoptimizedIr)
        val dumpIrString     = module.toString()
        val optimizedModule  = pipeline(module)
        val destroyed        = VerifySSA.run(SSADestructionFabric.create(optimizedModule).run())
        val optimizedCodegen = x64CodeGenerator.emit(destroyed)

        val directoryName = File(filename)
        if (directoryName.exists()) {
            println("[Directory '$directoryName' exist. Remove...]")
            directoryName.deleteRecursively()
        }

        val unoptimizedAsm = File("$filename/base.S")
        val optimizedAsm   = File("$filename/opt.S")
        val dumpIr         = File("$filename/$filename.ir")
        val baseDumpIrDestr = File("$filename/$filename.base.dest.ir")
        val optDumpIrOpt   = File("$filename/$filename.opt.ir")
        val optDumpIrDestr = File("$filename/$filename.opt.dest.ir")

        directoryName.mkdir()

        baseDumpIrDestr.writeText(unoptimizedIr.toString())
        unoptimizedAsm.writeText(unoptimisedCode.toString())
        optimizedAsm.writeText(optimizedCodegen.toString())
        dumpIr.writeText(dumpIrString)
        optDumpIrOpt.writeText(optimizedModule.toString())
        optDumpIrDestr.writeText(destroyed.toString())
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
}