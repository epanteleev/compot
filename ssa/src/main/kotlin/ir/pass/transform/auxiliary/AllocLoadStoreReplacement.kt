package ir.pass.transform.auxiliary

import collections.identityHashMapOf
import collections.identityHashSetOf
import ir.instruction.*
import ir.module.*
import ir.module.block.Block
import ir.pass.canBeReplaced
import ir.types.PrimitiveType
import java.util.*


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

    // store ptr %gen0x8, i32 0
    private fun replaceAllocLoadStores(loadAndStores: Set<Instruction>){
        for (bb in cfg.preorder()) {
            val instructions = bb.instructions()
            var i = -1
            while (i < instructions.size - 1) {
                i += 1
                val inst = instructions[i]
                when {
                    inst is Load && inst.operand() is Generate  -> replaceLoad(bb, inst, i)
                    inst is Store && inst.pointer() is Generate -> replaceStore(bb, inst, i)
                    inst is Copy && inst.origin() is Generate   -> replaceCopy(bb, inst, i)
                    else -> {
                        for (op in inst.operands()) {
                            if (op !is Generate) {
                                continue
                            }
                            val lea = bb.insert(i) { it.lea(op) }
                            ValueInstruction.replaceUsages(op, lea)
                            bb.remove(i + 1)
                        }
                    }
                }
            }
        }
    }

    private fun replaceAlloc(): Set<Instruction> {
        val loadAndStores = identityHashSetOf<Instruction>()
        for (bb in cfg) {
            val instructions = bb.instructions()
            var i = -1
            while (i < instructions.size - 1) {
                i += 1
                val inst = instructions[i]
                if (inst !is Alloc) {
                    continue
                }

                if (!inst.canBeReplaced()) {
                    continue
                }

                val gen = replaceAlloc(bb, inst, i)
                for (user in gen.usedIn()) {
                    if (user !is Load && user !is Store) {
                        continue
                    }

                    loadAndStores.add(user)
                }
            }
        }

        return loadAndStores
    }

    private fun pass() {
        val allocPointerUsers = replaceAlloc()
        replaceAllocLoadStores(allocPointerUsers)
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