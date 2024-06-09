package ir.pass.transform.auxiliary

import ir.*
import ir.types.*
import ir.module.*
import ir.instruction.*
import ir.module.block.Block
import ir.instruction.matching.*


class Lowering private constructor(private val cfg: BasicBlocks) {
    private fun replaceStore(bb: Block, inst: Store): Instruction {
        val toValue   = inst.pointer() as Generate
        val fromValue = inst.value()
        val move = bb.insertBefore(inst) { it.move(toValue, fromValue) }
        bb.kill(inst)
        return move
    }

    private fun replaceAlloc(bb: Block, inst: Alloc): Instruction {
        val gen = bb.insertBefore(inst) { it.gen(inst.allocatedType) }
        inst.replaceUsages(gen)
        bb.kill(inst)
        return gen
    }

    private fun replaceLoad(bb: Block, inst: Load): Instruction {
        val copy = bb.insertBefore(inst) { it.copy(inst.operand()) }
        inst.replaceUsages(copy)
        bb.kill(inst)
        return copy
    }

    private fun replaceCopy(bb: Block, inst: Copy): Instruction {
        val lea = bb.insertBefore(inst) { it.lea(inst.origin() as Generate) }
        inst.replaceUsages(lea)
        bb.kill(inst)
        return lea
    }

    private fun replaceAllocLoadStores() {
        fun closure(bb: Block, inst: Instruction): Instruction {
            when {
                store(generate(), nop()) (inst) -> return replaceStore(bb, inst as Store)
                load(generate()) (inst)         -> return replaceLoad(bb, inst as Load)
            }
            return inst
        }

        for (bb in cfg) {
            bb.transform { inst -> closure(bb, inst) }
        }
    }

    private fun replaceGepToLea() {
        fun closure(bb: Block, inst: Instruction): Instruction {
            if (inst !is LocalValue) {
                return inst
            }
            when {
                gep(generate(), nop()) (inst) -> {
                    inst as GetElementPtr
                    val lea = bb.insertBefore(inst) { it.leaStack(inst.source(), inst.basicType, inst.index()) }
                    inst.replaceUsages(lea)
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
                            inst.replaceUsages(lea)
                            bb.kill(inst)
                            return lea
                        }
                        is StructType -> {
                            val lea = bb.insertBefore(inst) {
                                it.leaStack(inst.source(), Type.U8, Constant.of(Type.U32, base.offset(inst.index().toInt())))
                            }
                            inst.replaceUsages(lea)
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

        fun closure(bb: Block, inst: Instruction): Instruction {
            when {
                store(gfpOrGep(generate().not(), nop()), nop()) (inst) -> {
                    inst as Store
                    val pointer = inst.pointer() as ValueInstruction
                    val move = bb.insertBefore(inst) { it.move(getSource(pointer), inst.value(), getIndex(pointer)) }
                    bb.kill(inst)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                    return move
                }
                store(gfpOrGep(generate(), nop()), nop()) (inst) -> {
                    inst as Store
                    val pointer = inst.pointer() as ValueInstruction
                    val st = bb.insertBefore(inst) { it.storeOnStack(getSource(pointer), getIndex(pointer), inst.value()) }
                    bb.kill(inst)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                    return st
                }
                load(gfpOrGep(generate().not(), nop())) (inst) -> {
                    inst as Load
                    val pointer = inst.operand() as ValueInstruction
                    val copy = bb.insertBefore(inst) { it.indexedLoad(getSource(pointer), inst.type(), getIndex(pointer)) }
                    inst.replaceUsages(copy)
                    bb.kill(inst)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                    return copy
                }
                load(gfpOrGep(generate(), nop())) (inst) -> {
                    inst as Load
                    val pointer = inst.operand() as ValueInstruction
                    val copy = bb.insertBefore(inst) { it.loadFromStack(getSource(pointer), inst.type(), getIndex(pointer)) }
                    inst.replaceUsages(copy)
                    bb.kill(inst)
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                    return copy
                }
            }
            return inst
        }

        for (bb in cfg) {
            bb.transform { inst -> closure(bb, inst) }
        }
    }

    private fun killOnDemand(bb: Block, instruction: LocalValue) {
        instruction as Instruction
        if (instruction.usedIn().isEmpty()) { //TODO Need DCE
            bb.kill(instruction) // TODO bb may not contain pointer
        }
    }

    private fun replaceByteDiv() {
        fun extend(bb: Block, inst: Instruction, value: Value): LocalValue {
            return when (val type = value.type()) {
                Type.I8 -> bb.insertBefore(inst) { it.sext(value, Type.I16) }
                else -> throw RuntimeException("type $type")
            }
        }

        fun closure(bb: Block, inst: Instruction): Instruction {
            when {
                binary(ArithmeticBinaryOp.Div, value(i8()), value(i8())) (inst) -> {
                    // Before:
                    //  %res = div i8 %a, %b
                    //
                    // After:
                    //  %extFirst  = sext %a to i16
                    //  %extSecond = sext %b to i16
                    //  %resI16 = div i16, %extFirst, %extSecond
                    //  %res = trunc i16 %resI16 to i8

                    inst as ArithmeticBinary
                    val extFirst  = extend(bb, inst, inst.first())
                    val extSecond = extend(bb, inst, inst.second())

                    val newDiv   = bb.insertBefore(inst) { it.arithmeticBinary(extFirst, ArithmeticBinaryOp.Div, extSecond) }
                    val truncate = bb.insertBefore(inst) { it.trunc(newDiv, Type.I8) }
                    inst.replaceUsages(truncate)
                    killOnDemand(bb, inst)
                    return newDiv
                }
                binary(ArithmeticBinaryOp.Div, constant().not(), constant()) (inst) -> {
                    // TODO temporal
                    inst as ArithmeticBinary
                    val second = inst.second()
                    val copy = bb.insertBefore(inst) { it.copy(second) }
                    inst.update(1, copy )
                    return inst
                }
                tupleDiv(constant().not(), constant()) (inst) -> {
                    // TODO temporal
                    inst as TupleDiv
                    val second = inst.second()
                    val copy = bb.insertBefore(inst) { it.copy(second) }
                    inst.update(1, copy )
                    return inst
                }
                tupleDiv(value(i8()), value(i8())) (inst) -> {
                    inst as TupleDiv

                    val extFirst  = extend(bb, inst, inst.first())
                    val extSecond = extend(bb, inst,  inst.second())
                    val newDiv = bb.insertBefore(inst) { it.tupleDiv(extFirst, extSecond) }
                    var last: Instruction = newDiv

                    val divProj = inst.proj(0)
                    if (divProj != null) {
                        val proj     = bb.insertBefore(inst) { it.proj(newDiv, 0) }
                        val truncate = bb.insertBefore(inst) { it.trunc(proj, Type.I8) }

                        divProj.replaceUsages(truncate)
                        killOnDemand(bb, divProj)
                        last = truncate
                    }

                    val remProj = inst.proj(1)
                    if (remProj != null) {
                        val proj     = bb.insertBefore(inst) { it.proj(newDiv, 1) }
                        val truncate = bb.insertBefore(inst) { it.trunc(proj, Type.I8) }

                        remProj.replaceUsages(truncate)
                        killOnDemand(bb, remProj)
                        last = truncate
                    }

                    killOnDemand(bb, inst)
                    return last
                }
            }
            return inst
        }

        for (bb in cfg.bfsTraversal()) {
            bb.transform { inst -> closure(bb, inst) }
        }
    }

    private fun replaceEscaped() {
        fun closure(bb: Block, inst: Instruction): Instruction {
            when {
                inst is Alloc && alloc() (inst) -> {
                    return replaceAlloc(bb, inst)
                }
                store(nop(), generate()) (inst) -> {
                    inst as Store
                    val lea = bb.insertBefore(inst) { it.lea(inst.value() as Generate) }
                    inst.update(1, lea)
                    return inst
                }
                copy(generate()) (inst) -> {
                    return replaceCopy(bb, inst as Copy)
                }
                ptr2int(generate()) (inst) -> {
                    inst as Pointer2Int
                    val lea = bb.insertBefore(inst) { it.lea(inst.value() as Generate) }
                    inst.update(0, lea)
                    return inst
                }
            }
            return inst
        }

        for (bb in cfg.bfsTraversal()) {
            bb.transform { inst -> closure(bb, inst) }
        }
    }

    private fun pass() {
        replaceByteDiv()
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