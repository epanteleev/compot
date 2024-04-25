package ir.pass.transform.auxiliary

import common.identityHashSetOf
import ir.module.*
import ir.instruction.*
import ir.module.block.Block
import ir.pass.canBeReplaced
import ir.types.PrimitiveType


class AllocLoadStoreReplacement private constructor(private val cfg: BasicBlocks) {
    private fun replaceStore(bb: Block, inst: Store, i: Int) {
        val toValue   = inst.pointer() as Generate
        val fromValue = inst.value()
        bb.insert(i) { it.move(toValue, fromValue) }
        bb.remove(i + 1)
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
        ValueInstruction.replaceUsages(inst, toValue)
        bb.remove(i)
    }

    private fun replaceCopy(bb: Block, inst: Copy, i: Int) {
        val lea = bb.insert(i) { it.copy(inst.origin()) }
        ValueInstruction.replaceUsages(inst, lea)
        bb.remove(i + 1)
    }

    private fun replaceAllocLoadStores(replaced: Set<Instruction>) {
        for (bb in cfg) {
            var idx = 0
            val instructions = bb.instructions()
            while (idx < instructions.size) {
                val inst = instructions[idx]
                if (inst !in replaced) {
                    idx++
                    continue
                }

                when (inst) {
                    is Store -> {
                        replaceStore(bb, inst, idx)
                        idx++
                    }
                    is Load -> replaceLoad(bb, inst, idx)
                    is Copy -> {
                        replaceCopy(bb, inst, idx)
                        idx++
                    }
                    else -> assert(false) { "should be, but inst=${inst}" }
                }
            }
        }
    }

    private fun replaceAlloc(): Set<Instruction> {
        val replaced = identityHashSetOf<Instruction>()

        fun replaceHelper(bb: Block, i: Int, inst: Instruction) {
            if (inst !is Alloc) {
                return
            }

            if (!inst.canBeReplaced()) {
                return
            }

            for (user in inst.usedIn()) {
                replaced.add(user)
            }

            val gen = replaceAlloc(bb, inst, i)
            for (user in gen.usedIn()) { //TODO: checker
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
        return replaced
    }

    private fun pass() {
        val replaced = replaceAlloc()
        replaceAllocLoadStores(replaced)
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