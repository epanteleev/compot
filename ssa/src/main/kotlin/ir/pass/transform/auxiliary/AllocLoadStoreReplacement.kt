package ir.pass.transform.auxiliary

import ir.module.*
import ir.instruction.*
import ir.instruction.matching.*
import ir.module.block.Block
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
        fun closure(bb: Block, inst: Instruction, i: Int): Int {
            when {
                store(generate(), nop()) (inst) -> replaceStore(bb, inst as Store, i)
                load(generate()) (inst)         -> replaceLoad(bb, inst as Load, i)
            }
            return 0
        }

        for (bb in cfg) {
            bb.forEachInstruction { i, inst -> closure(bb, inst, i) }
        }
    }

    private fun replaceGEPAndStore() {
        fun closure(bb: Block, inst: Instruction, i: Int): Int {
            val genOrAlloc = generate() or alloc()
            when {
                store(gep(genOrAlloc.not(), nop()), nop()) (inst) -> {
                    inst as Store
                    val pointer = inst.pointer() as GetElementPtr
                    bb.insert(i) { it.move(pointer.source(), inst.value(), pointer.index()) }
                    bb.remove(i + 1)
                }
                load(gep(genOrAlloc.not(), nop())) (inst) -> {
                    inst as Load
                    val pointer = inst.operand() as GetElementPtr
                    val copy = bb.insert(i) { it.indexedLoad(pointer.source(), inst.type(), pointer.index()) }
                    ValueInstruction.replaceUsages(inst, copy)
                    bb.remove(i + 1)
                }
            }
            return 0
        }

        for (bb in cfg) {
            bb.forEachInstruction { i, inst -> closure(bb, inst, i) }
        }
    }
    private fun replaceEscaped() {
        fun closure(bb: Block, inst: Instruction, i: Int): Int {
            when {
                inst is Alloc && alloc(primitive()) (inst) -> replaceAlloc(bb, inst, i)
                store(nop(), generate()) (inst) -> {
                    inst as Store
                    val lea = bb.insert(i) { it.lea(inst.value() as Generate) }
                    inst.update(1, lea)
                    return 1
                }
                copy(generate()) (inst) -> {
                    replaceCopy(bb, inst as Copy, i)
                    return 0
                }
                ptr2int(generate()) (inst) -> {
                    inst as Pointer2Int
                    val lea = bb.insert(i) { it.lea(inst.value() as Generate) }
                    inst.update(0, lea)
                    return 1
                }
            }
            return 0
        }

        for (bb in cfg.bfsTraversal()) {
            bb.forEachInstruction { i, inst -> closure(bb, inst, i) }
        }
    }

    private fun pass() {
        replaceEscaped()
        replaceAllocLoadStores()
        replaceGEPAndStore()
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