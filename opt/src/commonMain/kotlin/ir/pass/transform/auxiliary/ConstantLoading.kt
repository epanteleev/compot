package ir.pass.transform.auxiliary

import ir.global.*
import ir.instruction.Instruction
import ir.module.FunctionData
import ir.module.Module
import ir.module.block.Block
import ir.types.Type


internal class ConstantLoading private constructor(private val cfg: FunctionData) {
    private fun pass() {
        fun closure(bb: Block, inst: Instruction): Instruction {
            var inserted: Instruction? = null
            inst.operandsWithIndex { i, use ->
                if (use is AnyAggregateGlobalConstant || use is FunctionSymbol) {
                    val lea = bb.insertBefore(inst) { it.lea(use) }
                    bb.updateDF(inst, i, lea)
                    inserted = lea
                } else if (use is GlobalConstant) {
                    val lea = bb.insertBefore(inst) { it.copy(use) }
                    bb.updateDF(inst, i, lea)
                    inserted = lea
                } else if (use is ExternValue) {
                    val lea = bb.insertBefore(inst) { it.load(Type.Ptr, use) }
                    bb.updateDF(inst, i, lea)
                    inserted = lea
                }
            }
            return inserted?: inst
        }

        for (bb in cfg) {
            bb.transform { inst -> closure(bb, inst) }
        }
    }

    companion object {
        fun run(module: Module): Module {
            for (fn in module.functions()) {
                ConstantLoading(fn).pass()
            }
            return module
        }
    }
}