package ir.pass.transform.auxiliary

import ir.global.FunctionSymbol
import ir.global.GlobalConstant
import ir.global.GlobalValue
import ir.global.StringLiteralGlobal
import ir.instruction.Instruction
import ir.module.BasicBlocks
import ir.module.Module
import ir.module.block.Block


internal class ConstantLoading private constructor(private val cfg: BasicBlocks) {
    private fun pass() {
        fun closure(bb: Block, inst: Instruction): Instruction {
            var inserted: Instruction? = null
            for ((i, use) in inst.operands().withIndex()) {
                if (use !is StringLiteralGlobal) {
                    continue
                }

                val lea = bb.insertBefore(inst) { it.lea(use) }
                inst.update(i, lea)
                inserted = lea
            }
            return inserted?: inst
        }

        for (bb in cfg) {
            bb.transform { inst -> closure(bb, inst) }
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