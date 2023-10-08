package startup

import ir.Module

import ir.pass.ana.VerifySSA
import ir.pass.transform.Mem2Reg

import ir.read.ModuleReader
import ir.utils.DumpModule
import java.io.File
import kotlin.Throwable

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("<ir-file>.ir")
        return
    }

    val text = File(args[0]).readText()
    val module = ModuleReader(text).read()
    var opt: Module? = null

    try {
        Driver.output(args[0], module) {
            opt = it
            opt = Mem2Reg.run(opt as Module)
            opt = VerifySSA.run(opt as Module)
            opt as Module
        }
    } catch (ex: Throwable) {
        if (opt != null) {
            println(DumpModule.apply(opt!!))
        }

        ex.printStackTrace()
    }
}