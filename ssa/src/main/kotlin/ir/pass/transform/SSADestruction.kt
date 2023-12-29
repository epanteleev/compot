package ir.pass.transform

import ir.module.Module
import ir.pass.transform.auxiliary.CopyInsertion
import ir.platform.x64.pass.transform.MoveLargeConstants
import ir.pass.transform.auxiliary.SplitCriticalEdge
import ir.platform.x64.CSSAModule
import ir.platform.x64.pass.transform.ReplaceFloatNeg

class SSADestruction {
    companion object {
        fun run(module: Module): CSSAModule {
            val copy = module.copy()
            return MoveLargeConstants.run(ReplaceFloatNeg.run(CopyInsertion.run(SplitCriticalEdge.run(copy)))) as CSSAModule
        }
    }
}