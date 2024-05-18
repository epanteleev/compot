package ir.pass.transform.auxiliary

import ir.global.FunctionSymbol
import ir.global.GlobalConstant
import ir.module.BasicBlocks
import ir.module.Module


internal class ConstantLoading private constructor(private val cfg: BasicBlocks) {
    private fun pass() {
        for (bb in cfg) {
            bb.instructions { inst ->
                var inserted = 0
                for ((i, use) in inst.operands().withIndex()) {
                    if (use !is GlobalConstant && use !is FunctionSymbol) {
                        continue
                    }

                    val lea = bb.insertBefore(inst) { it.lea(use) }
                    inst.update(i, lea)
                    inserted++
                }
                inserted
            }
        }
    }

    companion object {
        fun run(module: Module): Module {
            for (fn in module.functions) {
                ConstantLoading(fn.blocks).pass()
            }
            return module
        }
    }
}