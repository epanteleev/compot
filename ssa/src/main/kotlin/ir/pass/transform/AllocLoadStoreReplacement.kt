package ir.pass.transform

import ir.module.*
import ir.instruction.*
import ir.module.block.Block
import ir.types.PrimitiveType
import ir.platform.x64.CSSAModule
import ir.pass.ValueInstructionExtension.isLocalVariable


class AllocLoadStoreReplacement private constructor(private val cfg: BasicBlocks) {
    private fun replaceStore(bb: Block, inst: Store, i: Int) {
        val toValue   = inst.pointer() as Generate
        val fromValue = inst.value()
        bb.remove(i)

        bb.insert(i) { it.move(toValue, fromValue) }
    }

    private fun replaceAlloc(bb: Block, inst: Alloc, i: Int) {
        val allocatedType = inst.allocatedType
        assert(inst.allocatedType is PrimitiveType) {
            "should be, but allocatedType=${allocatedType}"
        }
        allocatedType as PrimitiveType
        val gen = bb.insert(i) { it.gen(allocatedType) }

        ValueInstruction.replaceUsages(inst, gen)
        bb.remove(i + 1)
    }

    private fun replaceLoad(bb: Block, inst: Load, i: Int) {
        val toValue = inst.operand()
        val copy = bb.insert(i) { it.copy(toValue) }

        ValueInstruction.replaceUsages(inst, copy)
        bb.remove(i + 1)
    }

    private fun pass() {
        for (bb in cfg.preorder()) {
            val instructions = bb.instructions()
            val size = instructions.size
            for (i in 0 until size) {
                val inst = instructions[i]
                when {
                    inst is Alloc && inst.isLocalVariable() -> replaceAlloc(bb, inst, i)
                    inst is Load && inst.isLocalVariable() -> replaceLoad(bb, inst, i)
                    inst is Store && inst.isLocalVariable() -> replaceStore(bb, inst, i)
                    else -> {}
                }
            }
        }
    }

    companion object {
        fun run(module: Module): Module {
            val copy = module.copy()
            for (fn in copy.functions) {
                AllocLoadStoreReplacement(fn.blocks).pass()
            }
            val ret = CSSAModule(copy.functions, copy.externFunctions, copy.globals, copy.types)
            return ret
        }
    }
}