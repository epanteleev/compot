package startup

import ir.pass.ana.VerifySSA
import ir.pass.transform.Mem2Reg
import ir.read.ModuleReader
import ir.utils.DumpModule
import java.io.File

fun main(args: Array<String>) {
    val text = File(args[0]).readText()
    val module = ModuleReader(text).read()
    Driver.output(args[0], module) {
        VerifySSA.run(Mem2Reg.run(VerifySSA.run(it)))
    }
}