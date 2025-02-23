package ir.pass.transform.auxiliary

import ir.global.*
import ir.instruction.Callable
import ir.instruction.Copy
import ir.instruction.GetElementPtr
import ir.instruction.GetFieldPtr
import ir.module.*
import ir.instruction.Instruction
import ir.instruction.IntCompare
import ir.instruction.Load
import ir.instruction.Memcpy
import ir.instruction.Phi
import ir.instruction.Pointer2Int
import ir.instruction.Return
import ir.instruction.Store
import ir.instruction.lir.Lea
import ir.module.block.Block
import ir.types.PtrType


internal class ConstantLoading private constructor(private val cfg: FunctionData) {
    private fun isIt(inst: Instruction) {
        if (inst !is Load &&
            inst !is Copy &&
            inst !is IntCompare &&
            inst !is Phi &&
            inst !is Return &&
            inst !is Callable &&
            inst !is Memcpy &&
            inst !is Store &&
            inst !is GetFieldPtr &&
            inst !is GetElementPtr &&
            inst !is Pointer2Int) {
            throw IllegalStateException("Unexpected instruction: ${inst.dump()}")
        }
    }
    private fun pass() {
        fun closure(bb: Block, inst: Instruction): Instruction {
            for ((i ,use) in inst.operands().withIndex()) {
                when (use) {
                    is AnyAggregateGlobalConstant, is FunctionPrototype -> {
                        val lea = bb.putBefore(inst, Lea.lea(use))
                        bb.updateDF(inst, i, lea)
                        isIt(inst)
                    }
                    is ExternValue, is ExternFunction -> {
                        val lea = bb.putBefore(inst, Load.load(PtrType, use))
                        bb.updateDF(inst, i, lea)
                        isIt(inst)
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