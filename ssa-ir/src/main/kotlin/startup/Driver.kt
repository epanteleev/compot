package startup

import ir.*
import ir.codegen.x64.CodeEmitter
import ir.utils.DumpModule
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object Driver {
    fun output(name: String, module: Module, pipeline: (Module) -> Module) {
        val filename         = getName(name)
        val codegen          = CodeEmitter.codegen(module)
        val dumpIrString     = DumpModule.apply(module)
        val optimizedModule  = pipeline(module)
        val optimizedCodegen = CodeEmitter.codegen(optimizedModule)

        val directoryName = File(filename)
        if (directoryName.exists()) {
            println("[Directory '$directoryName' exist. Remove...]")
            directoryName.deleteRecursively()
        }

        val unoptimizedAsm = File("$filename/base.S")
        val optimizedAsm   = File("$filename/opt.S")
        val dumpIr         = File("$filename/$filename.ir")
        val dumpIrOpt      = File("$filename/$filename.opt.ir")

        val objectFileBase = File("$filename/base.o")
        val optObjFileBase = File("$filename/opt.o")

        directoryName.mkdir()

        unoptimizedAsm.writeText(codegen.toString())
        optimizedAsm.writeText(optimizedCodegen.toString())
        dumpIr.writeText(dumpIrString)
        dumpIrOpt.writeText(DumpModule.apply(optimizedModule))

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