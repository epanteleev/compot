package ir.pass.transform.auxiliary

import ir.module.*
import ir.instruction.*
import ir.module.block.Block
import ir.pass.canBeReplaced
import ir.types.PrimitiveType


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

    private fun replaceCopy(bb: Block, inst: Copy, i: Int) {
        val lea = bb.insert(i) { it.lea(inst.origin() as Generate) }
        ValueInstruction.replaceUsages(inst, lea)
        bb.remove(i + 1)
    }

    private fun replaceAllocLoadStores(): Set<Copy> {
        val allocPointerUsers = mutableSetOf<Copy>()
        for (bb in cfg.preorder()) {
            val instructions = bb.instructions()
            val size = instructions.size
            for (i in 0 until size) {
                val inst = instructions[i]
                when {
                    inst is Alloc && inst.canBeReplaced() -> {
                        for (user in inst.usedIn()) {
                            if (user !is Copy) {
                                continue
                            }

                            allocPointerUsers.add(user)
                        }

                        replaceAlloc(bb, inst, i)
                    }
                    inst is Load && inst.canBeReplaced() -> replaceLoad(bb, inst, i)
                    inst is Store && inst.canBeReplaced() -> replaceStore(bb, inst, i)
                    inst is Copy && allocPointerUsers.contains(inst) -> replaceCopy(bb, inst, i)
                    else -> {}
                }
            }
        }

        return allocPointerUsers
    }

    private fun pass() {
        replaceAllocLoadStores()


    }

    companion object {
        fun run(module: Module): Module {
            for (fn in module.functions) {
                AllocLoadStoreReplacement(fn.blocks).pass()
            }
            return module
        }
    }
}