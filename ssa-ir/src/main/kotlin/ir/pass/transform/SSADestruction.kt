package ir.pass.transform

import ir.module.Module
import ir.pass.transform.auxiliary.CopyInsertion
import ir.pass.transform.auxiliary.SplitCriticalEdge
import ir.platform.x64.CSSAModule

class SSADestruction {
    companion object {
        fun run(module: Module): CSSAModule {
            val copy = module.copy()
            return CopyInsertion.run(SplitCriticalEdge.run(copy)) as CSSAModule
        }
    }
}