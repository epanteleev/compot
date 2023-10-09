package ir.pass.transform

import ir.*
import ir.pass.transform.auxiliary.CopyInsertion
import ir.pass.transform.auxiliary.SplitCriticalEdge

class SSADestruction {
    companion object {
        fun run(module: Module): Module {
            val copy = module.copy()
            return CopyInsertion.run(SplitCriticalEdge.run(copy))
        }
    }
}