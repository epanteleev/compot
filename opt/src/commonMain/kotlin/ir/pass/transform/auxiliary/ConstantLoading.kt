package ir.pass.transform.auxiliary

import ir.global.*
import ir.instruction.Instruction
import ir.instruction.Load
import ir.instruction.lir.Lea
import ir.module.ExternFunction
import ir.module.FunctionData
import ir.module.FunctionPrototype
import ir.module.Module
import ir.module.block.Block
import ir.types.PtrType


internal class ConstantLoading private constructor(private val cfg: FunctionData) {
    private fun pass() {
        fun closure(bb: Block, inst: Instruction): Instruction {
            for ((i ,use) in inst.operands().withIndex()) {
                when (use) {
                    is AnyAggregateGlobalConstant, is FunctionPrototype -> {
                        val lea = bb.putBefore(inst, Lea.lea(use))
                        bb.updateDF(inst, i, lea)
                    }
                    is ExternValue, is ExternFunction -> {
                        val lea = bb.putBefore(inst, Load.load(PtrType, use))
                        bb.updateDF(inst, i, lea)
                    }
                }
            }
            return inst
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