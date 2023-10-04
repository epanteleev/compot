package startup

import ir.codegen.x64.CodeEmitter
import ir.codegen.x64.regalloc.Coalescing
import ir.codegen.x64.regalloc.LinearScan
import ir.pass.ana.VerifySSA
import ir.pass.transform.CopyInsertion
import ir.pass.transform.Mem2Reg
import ir.pass.transform.SplitCriticalEdge
import ir.read.ModuleReader
import ir.utils.DumpModule
import ir.codegen.x64.regalloc.liveness.Liveness
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("<ir-file>.ir")
        return
    }

    val text = File(args[0]).readText()
    val module = ModuleReader(text).read()
    Driver.output(args[0], module) {
        VerifySSA.run(Mem2Reg.run(it))
    }
}