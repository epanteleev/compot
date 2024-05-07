package ir.pass.transform.auxiliary

import common.identityHashSetOf
import ir.module.*
import ir.instruction.*
import ir.module.block.Block
import ir.pass.canBeReplaced
import ir.types.PrimitiveType


class AllocLoadStoreReplacement private constructor(private val cfg: BasicBlocks) {
    private val replaced = identityHashSetOf<Instruction>()
    private val escaped = identityHashSetOf<Instruction>()

    init {
        replaceAlloc()
    }

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
        val copy = bb.insert(i) { it.copy(inst.operand()) }
        ValueInstruction.replaceUsages(inst, copy)
        bb.remove(i + 1)
    }

    private fun replaceCopy(bb: Block, inst: Copy, i: Int) {
        val lea = bb.insert(i) { it.lea(inst.origin() as Generate) }
        ValueInstruction.replaceUsages(inst, lea)
        bb.remove(i + 1)
    }

    private fun replaceAllocLoadStores() {
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
                    is Load -> {
                        replaceLoad(bb, inst, idx)
                        idx++
                    }
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
        fun replaceHelper(bb: Block, i: Int, inst: Instruction) {
            if (inst !is Alloc) {
                return
            }

            if (!inst.canBeReplaced()) {
                return
            }

            for (user in inst.usedIn()) {
                if (user is Load || (user is Store && user.pointer() == inst)) {
                    replaced.add(user)
                    continue
                }

                escaped.add(user)
            }

            replaceAlloc(bb, inst, i)
        }

        for (bb in cfg) {
            bb.forEachInstruction { i, inst -> replaceHelper(bb, i, inst)}
        }
        return replaced
    }

    private fun pass() {
        for (bb in cfg) {
            var idx = 0
            val instructions = bb.instructions()
            while (idx < instructions.size) {
                val inst = instructions[idx]
                if (!escaped.contains(inst)) {
                    idx++
                    continue
                }

                when (inst) {
                    is Store -> {
                        val lea = bb.insert(idx) { it.lea(inst.value() as Generate) }
                        inst.update(1, lea)
                        idx++
                    }
                    is Copy -> {
                        val lea = bb.insert(idx) { it.lea(inst.origin() as Generate) }
                        ValueInstruction.replaceUsages(inst, lea)
                        bb.remove(idx + 1)
                    }
                    is Pointer2Int -> {
                        val lea = bb.insert(idx) { it.lea(inst.value() as Generate) }
                        inst.update(0, lea)
                        idx++
                    }
                    else -> assert(false) { "always unreachable, inst=${inst.dump()}" }
                }
                idx++
            }
        }

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