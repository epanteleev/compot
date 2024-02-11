package ir.pass.transform

import ir.module.Module
import ir.platform.x64.CSSAModule
import ir.pass.transform.auxiliary.*
import ir.platform.x64.pass.transform.ReplaceFloatNeg
import ir.platform.x64.pass.transform.MoveLargeConstants


class SSADestruction {
    companion object {
        fun run(module: Module): CSSAModule {
            val copy = module.copy()
            val transformed = AllocLoadStoreReplacement.run(
                MoveLargeConstants.run(
                    ReplaceFloatNeg.run(
                        CopyInsertion.run(
                            SplitCriticalEdge.run(copy)
                        )
                    )
                )
            )

            return transformed as CSSAModule
        }
    }
}