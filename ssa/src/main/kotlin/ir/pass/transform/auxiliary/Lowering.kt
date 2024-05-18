package ir.pass.transform.auxiliary

import ir.*
import ir.types.*
import ir.module.*
import ir.instruction.*
import ir.module.block.Block
import ir.instruction.matching.*


class Lowering private constructor(private val cfg: BasicBlocks) {
    private fun replaceStore(bb: Block, inst: Store) {
        val toValue   = inst.pointer() as Generate
        val fromValue = inst.value()
        bb.insertBefore(inst) { it.move(toValue, fromValue) }
        bb.kill(inst)
    }

    private fun replaceAlloc(bb: Block, inst: Alloc) {
        val gen = bb.insertBefore(inst) { it.gen(inst.allocatedType) }
        ValueInstruction.replaceUsages(inst, gen)
        bb.kill(inst)
    }

    private fun replaceLoad(bb: Block, inst: Load) {
        val copy = bb.insertBefore(inst) { it.copy(inst.operand()) }
        ValueInstruction.replaceUsages(inst, copy)
        bb.kill(inst)
    }

    private fun replaceCopy(bb: Block, inst: Copy) {
        val lea = bb.insertBefore(inst) { it.lea(inst.origin() as Generate) }
        ValueInstruction.replaceUsages(inst, lea)
        bb.kill(inst)
    }

    private fun replaceAllocLoadStores() {
        fun closure(bb: Block, inst: Instruction): Int {
            when {
                store(generate(), nop()) (inst) -> replaceStore(bb, inst as Store)
                load(generate()) (inst)         -> replaceLoad(bb, inst as Load)
            }
            return 0
        }

        for (bb in cfg) {
            bb.instructions { inst -> closure(bb, inst) }
        }
    }

    private fun replaceGepToLea() {
        fun closure(bb: Block, inst: Instruction): Instruction {
            if (inst !is ValueInstruction) {
                return inst
            }
            when {
                gep(generate(), nop()) (inst) -> {
                    inst as GetElementPtr
                    val lea = bb.insertBefore(inst) { it.leaStack(inst.source(), inst.basicType, inst.index()) }
                    ValueInstruction.replaceUsages(inst, lea)
                    bb.kill(inst)
                    return lea
                }
                gfp(generate(), nop()) (inst) -> {
                    inst as GetFieldPtr

                    when (val base = inst.basicType) {
                        is ArrayType -> {
                            val lea = bb.insertBefore(inst) {
                                it.leaStack(inst.source(), base.elementType() as PrimitiveType, inst.index())
                            }
                            ValueInstruction.replaceUsages(inst, lea)
                            bb.kill(inst)
                            return lea
                        }
                        is StructType -> {
                            val lea = bb.insertBefore(inst) {
                                it.leaStack(inst.source(), Type.U8, Constant.of(Type.U32, base.offset(inst.index().toInt())))
                            }
                            ValueInstruction.replaceUsages(inst, lea)
                            bb.kill(inst)
                            return lea
                        }
                    }
                }
            }
            return inst
        }

        for (bb in cfg) {
            bb.transform { inst -> closure(bb, inst) }
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

        fun closure(bb: Block, inst: Instruction): Int {
            when {
                store(gfpOrGep(generate().not(), nop()), nop()) (inst) -> {
                    inst as Store
                    val pointer = inst.pointer() as ValueInstruction
                    bb.insertBefore(inst) { it.move(getSource(pointer), inst.value(), getIndex(pointer)) }
                    bb.kill(inst)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                }
                store(gfpOrGep(generate(), nop()), nop()) (inst) -> {
                    inst as Store
                    val pointer = inst.pointer() as ValueInstruction
                    bb.insertBefore(inst) { it.storeOnStack(getSource(pointer), getIndex(pointer), inst.value()) }
                    bb.kill(inst)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                }
                load(gfpOrGep(generate().not(), nop())) (inst) -> {
                    inst as Load
                    val pointer = inst.operand() as ValueInstruction
                    val copy = bb.insertBefore(inst) { it.indexedLoad(getSource(pointer), inst.type(), getIndex(pointer)) }
                    ValueInstruction.replaceUsages(inst, copy)
                    bb.kill(inst)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                }
                load(gfpOrGep(generate(), nop())) (inst) -> {
                    inst as Load
                    val pointer = inst.operand() as ValueInstruction
                    val copy = bb.insertBefore(inst) { it.loadFromStack(getSource(pointer), inst.type(), getIndex(pointer)) }
                    ValueInstruction.replaceUsages(inst, copy)
                    bb.kill(inst)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                }
            }
            return 0
        }

        for (bb in cfg) {
            bb.instructions { inst -> closure(bb, inst) }
        }
    }
    private fun replaceEscaped() {
        fun closure(bb: Block, inst: Instruction): Int {
            when {
                inst is Alloc && alloc() (inst) -> replaceAlloc(bb, inst)
                store(nop(), generate()) (inst) -> {
                    inst as Store
                    val lea = bb.insertBefore(inst) { it.lea(inst.value() as Generate) }
                    inst.update(1, lea)
                    return 1
                }
                copy(generate()) (inst) -> {
                    replaceCopy(bb, inst as Copy)
                    return 0
                }
                ptr2int(generate()) (inst) -> {
                    inst as Pointer2Int
                    val lea = bb.insertBefore(inst) { it.lea(inst.value() as Generate) }
                    inst.update(0, lea)
                    return 1
                }
            }
            return 0
        }

        for (bb in cfg.bfsTraversal()) {
            bb.instructions { inst -> closure(bb, inst) }
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