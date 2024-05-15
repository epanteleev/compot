package ir.pass.transform.auxiliary

import ir.*
import ir.types.*
import ir.module.*
import ir.instruction.*
import ir.module.block.Block
import ir.instruction.matching.*


class Lowering private constructor(private val cfg: BasicBlocks) {
    private fun replaceStore(bb: Block, inst: Store, i: Int) {
        val toValue   = inst.pointer() as Generate
        val fromValue = inst.value()
        bb.insert(i) { it.move(toValue, fromValue) }
        bb.remove(i + 1)
    }

    private fun replaceAlloc(bb: Block, inst: Alloc, i: Int): Generate {
        val gen = bb.insert(i) { it.gen(inst.allocatedType) }
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

    private fun replaceGepToLea() {
        fun closure(bb: Block, inst: Instruction, i: Int): Int {
            if (inst !is ValueInstruction) {
                return 0
            }
            when {
                gep(generate(), nop()) (inst) -> {
                    inst as GetElementPtr
                    val lea = bb.insert(i) { it.leaStack(inst.source(), inst.basicType, inst.index()) }
                    ValueInstruction.replaceUsages(inst, lea)
                    bb.remove(i + 1)
                }
                gfp(generate(), nop()) (inst) -> {
                    inst as GetFieldPtr

                    when (val base = inst.basicType) {
                        is ArrayType -> {
                            val lea = bb.insert(i) {
                                it.leaStack(inst.source(), base.elementType() as PrimitiveType, inst.index())
                            }
                            ValueInstruction.replaceUsages(inst, lea)
                            bb.remove(i + 1)
                        }
                        is StructType -> {
                            val lea = bb.insert(i) {
                                it.leaStack(inst.source(), Type.U8, Constant.of(Type.U32, base.offset(inst.index().toInt())))
                            }
                            ValueInstruction.replaceUsages(inst, lea)
                            bb.remove(i + 1)
                        }
                    }
                }
            }
            return 0
        }

        for (bb in cfg) {
            bb.forEachInstruction { i, inst -> closure(bb, inst, i) }
        }
    }
    private fun replaceGEPAndStore() {
        fun getSource(inst: Instruction): Value {
            return when (inst) {
                is GetElementPtr -> inst.source()
                is GetFieldPtr   -> inst.source()
                else             -> throw IllegalArgumentException("Expected GEP or GFP")
            }
        }

        fun getIndex(inst: Instruction): Value {
            return when (inst) {
                is GetElementPtr -> inst.index()
                is GetFieldPtr   -> inst.index()
                else             -> throw IllegalArgumentException("Expected GEP or GFP")
            }
        }

        fun closure(bb: Block, inst: Instruction, i: Int): Int {
            when {
                store(gfpOrGep(generate().not(), nop()), nop()) (inst) -> {
                    inst as Store
                    val pointer = inst.pointer() as ValueInstruction
                    bb.insert(i) { it.move(getSource(pointer), inst.value(), getIndex(pointer)) }
                    bb.remove(i + 1)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.remove(bb.indexOf(pointer))
                    }
                }
                store(gfpOrGep(generate(), nop()), nop()) (inst) -> {
                    inst as Store
                    val pointer = inst.pointer() as ValueInstruction
                    bb.insert(i) { it.storeOnStack(getSource(pointer), getIndex(pointer), inst.value()) }
                    bb.remove(i + 1)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.remove(bb.indexOf(pointer))
                    }
                }
                load(gfpOrGep(generate().not(), nop())) (inst) -> {
                    inst as Load
                    val pointer = inst.operand() as ValueInstruction
                    val copy = bb.insert(i) { it.indexedLoad(getSource(pointer), inst.type(), getIndex(pointer)) }
                    ValueInstruction.replaceUsages(inst, copy)
                    bb.remove(i + 1)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.remove(bb.indexOf(pointer))
                    }
                }
                load(gfpOrGep(generate(), nop())) (inst) -> {
                    inst as Load
                    val pointer = inst.operand() as ValueInstruction
                    val copy = bb.insert(i) { it.loadFromStack(getSource(pointer), inst.type(), getIndex(pointer)) }
                    ValueInstruction.replaceUsages(inst, copy)
                    bb.remove(i + 1)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.remove(bb.indexOf(pointer))
                    }
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
                inst is Alloc && alloc() (inst) -> replaceAlloc(bb, inst, i)
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
        replaceGepToLea()
    }

    companion object {
        fun run(module: Module): Module {
            for (fn in module.functions) {
                Lowering(fn.blocks).pass()
            }
            return module
        }
    }
}