package ir.pass.transform.auxiliary

import ir.types.*
import ir.value.*
import ir.module.*
import ir.instruction.*
import ir.instruction.Store.Companion.VALUE
import ir.module.block.Block
import ir.instruction.matching.*


class Lowering private constructor(private val cfg: FunctionData) {
    private fun replaceAllocLoadStores() {
        fun closure(bb: Block, inst: Instruction): Instruction? = when {
            store(generate(), nop()) (inst) -> { inst as Store
                val toValue = inst.pointer().asValue<Generate>()
                bb.replace(inst) { it.move(toValue, inst.value()) }
            }
            load(generate()) (inst) -> { inst as Load
                val fromValue = inst.operand().asValue<Generate>()
                bb.replace(inst) { it.copy(fromValue) }
            }
            else -> inst
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
                gep(generate(), nop()) (inst) -> { inst as GetElementPtr
                    when (val baseType = inst.basicType) {
                        is AggregateType -> {
                            val index = inst.index()
                            val offset = bb.insertBefore(inst) {
                                it.arithmeticBinary(index, ArithmeticBinaryOp.Mul, Constant.of(index.type(), baseType.sizeOf()))
                            }
                            return bb.replace(inst) { it.leaStack(inst.source(), Type.I8, offset) }
                        }
                        else -> {
                            baseType as PrimitiveType
                            return bb.replace(inst) { it.leaStack(inst.source(), baseType, inst.index()) }
                        }
                    }

                }
                gfp(generate()) (inst) -> { inst as GetFieldPtr
                    return when (val base = inst.basicType) {
                        is ArrayType -> {
                            when (val tp = base.elementType()) {
                                is AggregateType -> {
                                    val index = inst.index(0).toInt()
                                    val offset = tp.offset(index)
                                    bb.replace(inst) { it.leaStack(inst.source(), Type.I8, Constant.of(Type.U32, offset)) }
                                }
                                else -> {
                                    tp as PrimitiveType
                                    bb.replace(inst) { it.leaStack(inst.source(), tp, inst.index(0)) }
                                }
                            }
                        }
                        is StructType -> bb.replace(inst) { it.leaStack(inst.source(), Type.U8, Constant.of(Type.U32, base.offset(inst.index(0).toInt()))) }
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
                is GetFieldPtr -> {
                    val index = inst.index(0).toInt()
                    val field = inst.basicType.field(index)
                    U64Value(inst.basicType.offset(index).toLong() / field.sizeOf())
                }
                else -> throw IllegalArgumentException("Expected GEP or GFP")
            }
        }

        fun closure(bb: Block, inst: Instruction): Instruction {
            when {
                store(gfpOrGep(generate().not(), nop()), nop()) (inst) -> { inst as Store
                    val pointer = inst.pointer().asValue<ValueInstruction>()
                    val move = bb.replace(inst) { it.move(getSource(pointer), getIndex(pointer), inst.value()) }
                    killOnDemand(bb, pointer)
                    return move
                }
                store(gfpOrGep(generate(), nop()), nop()) (inst) -> { inst as Store
                    val pointer = inst.pointer().asValue<ValueInstruction>()
                    val st = bb.replace(inst) { it.storeOnStack(getSource(pointer), getIndex(pointer), inst.value()) }
                    killOnDemand(bb, pointer)
                    return st
                }
                load(gfpOrGep(generate().not(), nop())) (inst) -> { inst as Load
                    val pointer = inst.operand().asValue<ValueInstruction>()
                    val copy = bb.replace(inst) { it.indexedLoad(getSource(pointer), inst.type(), getIndex(pointer)) }
                    killOnDemand(bb, pointer)
                    return copy
                }
                load(gfpOrGep(generate(), nop())) (inst) -> { inst as Load
                    val pointer = inst.operand().asValue<ValueInstruction>()
                    val index = getIndex(pointer)
                    val copy = bb.replace(inst) { it.loadFromStack(getSource(pointer), inst.type(), index) }
                    killOnDemand(bb, pointer)
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
            bb.kill(instruction, Value.UNDEF) // TODO bb may not contain pointer
        }
    }

    private fun replaceByteDiv() {
        fun closure(bb: Block, inst: Instruction): Instruction {
            when {
                binary(ArithmeticBinaryOp.Div, value(i8()), value(i8())) (inst) -> { inst as ArithmeticBinary
                    // Before:
                    //  %res = div i8 %a, %b
                    //
                    // After:
                    //  %extFirst  = sext %a to i16
                    //  %extSecond = sext %b to i16
                    //  %resI16 = div i16, %extFirst, %extSecond
                    //  %res = trunc i16 %resI16 to i8

                    val extFirst  = bb.insertBefore(inst) { it.sext(inst.first(), Type.I16) }
                    val extSecond = bb.insertBefore(inst) { it.sext(inst.second(), Type.I16) }
                    val newDiv = bb.insertBefore(inst) { it.arithmeticBinary(extFirst, ArithmeticBinaryOp.Div, extSecond) }
                    bb.replace(inst) { it.trunc(newDiv, Type.I8) }
                    return newDiv
                }
                binary(ArithmeticBinaryOp.Div, constant().not(), constant()) (inst) -> { inst as ArithmeticBinary
                    // TODO temporal
                    val second = inst.second()
                    val copy = bb.insertBefore(inst) { it.copy(second) }
                    bb.updateDF(inst, ArithmeticBinary.SECOND, copy)
                    return inst
                }
                tupleDiv(constant().not(), constant()) (inst) -> { inst as TupleDiv
                    // TODO temporal
                    val second = inst.second()
                    val copy = bb.insertBefore(inst) { it.copy(second) }
                    bb.updateDF(inst, TupleDiv.SECOND, copy)
                    return inst
                }
                tupleDiv(value(i8()), value(i8())) (inst) -> { inst as TupleDiv
                    // Before:
                    //  %resANDrem = div i8 %a, %b
                    //
                    // After:
                    //  %extFirst  = sext %a to i16
                    //  %extSecond = sext %b to i16
                    //  %newDiv = div i16 %extFirst, %extSecond
                    //  %projDiv = proj %newDiv, 0
                    //  %projRem = proj %newDiv, 1
                    //  %res = trunc %projDiv to i8
                    //  %rem = trunc %projRem to i8

                    val extFirst  = bb.insertBefore(inst) { it.sext(inst.first(), Type.I16) }
                    val extSecond = bb.insertBefore(inst) { it.sext(inst.second(), Type.I16) }
                    val newDiv    = bb.insertBefore(inst) { it.tupleDiv(extFirst, extSecond) }
                    var last: Instruction = newDiv

                    val divProj = inst.proj(0)
                    if (divProj != null) {
                        val proj     = bb.insertBefore(inst) { it.proj(newDiv, 0) }
                        val truncate = bb.updateOf(divProj) {
                            bb.insertBefore(inst) { it.trunc(proj, Type.I8) }
                        }
                        killOnDemand(bb, divProj)
                        last = truncate
                    }

                    val remProj = inst.proj(1)
                    if (remProj != null) {
                        val proj     = bb.insertBefore(inst) { it.proj(newDiv, 1) }
                        val truncate = bb.updateOf(remProj) {
                            bb.insertBefore(inst) { it.trunc(proj, Type.I8) }
                        }
                        killOnDemand(bb, remProj)
                        last = truncate
                    }

                    killOnDemand(bb, inst)
                    return last
                }
                select(nop(), value(i8()), value(i8())) (inst) -> { inst as Select
                    // Before:
                    //  %res = select i1 %cond, i8 %onTrue, i8 %onFalse
                    //
                    // After:
                    //  %extOnTrue  = sext %onTrue to i16
                    //  %extOnFalse = sext %onFalse to i16
                    //  %newSelect = select i1 %cond, i16 %extOnTrue, i16 %extOnFalse
                    //  %res = trunc %newSelect to i8

                    val extOnTrue  = bb.insertBefore(inst) { it.sext(inst.onTrue(), Type.I16) }
                    val extOnFalse = bb.insertBefore(inst) { it.sext(inst.onFalse(), Type.I16) }
                    val newSelect  = bb.insertBefore(inst) { it.select(inst.condition(), Type.I16, extOnTrue, extOnFalse) }
                    bb.replace(inst) { it.trunc(newSelect, Type.I8) }
                    return newSelect
                }
                select(nop(), value(u8()), value(u8())) (inst) -> { inst as Select
                    // Before:
                    //  %res = select i1 %cond, i8 %onTrue, i8 %onFalse
                    //
                    // After:
                    //  %extOnTrue  = zext %onTrue to i16
                    //  %extOnFalse = zext %onFalse to i16
                    //  %newSelect = select i1 %cond, i16 %extOnTrue, i16 %extOnFalse
                    //  %res = trunc %newSelect to i8

                    val extOnTrue  = bb.insertBefore(inst) { it.zext(inst.onTrue(), Type.U16) }
                    val extOnFalse = bb.insertBefore(inst) { it.zext(inst.onFalse(), Type.U16) }
                    val newSelect  = bb.insertBefore(inst) { it.select(inst.condition(), Type.U16, extOnTrue, extOnFalse) }
                    bb.replace(inst) { it.trunc(newSelect, Type.U8) }
                    return newSelect
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
                alloc() (inst) -> { inst as Alloc
                    return bb.replace(inst) { it.gen(inst.allocatedType) }
                }
                store(nop(), generate()) (inst) -> { inst as Store
                    val lea = bb.insertBefore(inst) { it.lea(inst.value() as Generate) }
                    bb.updateDF(inst, VALUE, lea)
                    return inst
                }
                copy(generate()) (inst) -> { inst as Copy
                    return bb.replace(inst) { it.lea(inst.origin() as Generate) }
                }
                ptr2int(generate()) (inst) -> { inst as Pointer2Int
                    val lea = bb.insertBefore(inst) { it.lea(inst.value() as Generate) }
                    bb.updateDF(inst, Pointer2Int.SOURCE, lea)
                    return inst
                }
                // TODO memcpy can be replaced with moves in some cases
                memcpy(nop(), generate(), nop()) (inst) -> { inst as Memcpy
                    val src = bb.insertBefore(inst) { it.lea(inst.source().asValue<Generate>()) }
                    bb.updateDF(inst, Memcpy.SOURCE, src)
                    return inst.prev() as Instruction //TODO refactor
                }
                memcpy(generate(), nop(), nop()) (inst) -> { inst as Memcpy
                    val dst = bb.insertBefore(inst) { it.lea(inst.destination().asValue<Generate>()) }
                    bb.updateDF(inst, Memcpy.DESTINATION, dst)
                    return inst.prev() as Instruction //TODO refactor
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
                Lowering(fn).pass()
            }
            return module
        }
    }
}