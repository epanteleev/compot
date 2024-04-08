package ir.pass.transform.auxiliary

import ir.instruction.*
import ir.module.*
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

    private fun replaceAlloc(bb: Block, inst: Alloc, i: Int): Generate {
        val allocatedType = inst.allocatedType
        assert(inst.allocatedType is PrimitiveType) {
            "should be, but allocatedType=${allocatedType}"
        }
        allocatedType as PrimitiveType
        val gen = bb.insert(i) { it.gen(allocatedType) }

        ValueInstruction.replaceUsages(inst, gen)
        bb.remove(i + 1)
        return gen
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

    private fun replaceAllocLoadStores() {
        fun replaceHelper(bb: Block, i: Int, instruction: Instruction) {
            when (instruction) {
                is Copy -> {
                    if (instruction.origin() is Generate) {
                        replaceCopy(bb, instruction, i)
                    }
                }
                is Load -> {
                    if (instruction.canBeReplaced()) {
                        replaceLoad(bb, instruction, i)
                    }
                }
                is Store -> {
                    if (instruction.pointer() !is Generate) {
                        return
                    }

                    val value = instruction.value()
                    if (value is Generate) {
                        val lea = bb.insert(i) { it.lea(value) }
                        instruction.update(1, lea)
                        replaceStore(bb, instruction, i + 1)
                    } else {
                        replaceStore(bb, instruction, i)
                    }
                }
            }
        }

        for (bb in cfg) {
            bb.forEachInstruction { i, inst -> replaceHelper(bb, i, inst) }
        }
    }

    private fun replaceAlloc() {
        fun replaceHelper(bb: Block, i: Int, inst: Instruction) {
            if (inst !is Alloc) {
                return
            }

            if (!inst.canBeReplaced()) {
                return
            }

            val gen = replaceAlloc(bb, inst, i)
            for (user in gen.usedIn()) {
                if (user !is Load && user !is Store) {
                    assert(user is Copy) {
                        "should be, but user=${user}"
                    }
                    continue
                }
            }
        }

        for (bb in cfg) {
            bb.forEachInstruction { i, inst -> replaceHelper(bb, i, inst)}
        }
    }

    private fun pass() {
        replaceAlloc()
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