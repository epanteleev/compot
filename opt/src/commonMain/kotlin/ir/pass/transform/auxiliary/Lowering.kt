package ir.pass.transform.auxiliary

import ir.types.*
import ir.value.*
import ir.module.*
import ir.instruction.*
import ir.module.block.Block
import ir.instruction.matching.*


class Lowering private constructor(private val cfg: BasicBlocks) {
    private fun replaceStore(bb: Block, inst: Store): Instruction {
        val toValue   = inst.pointer().asValue<Generate>()
        val fromValue = inst.value()
        val mv = bb.update(inst) { it.move(toValue, fromValue) }
        return mv
    }

    private fun replaceAlloc(bb: Block, inst: Alloc): Instruction {
        return bb.update(inst) { it.gen(inst.allocatedType) }
    }

    private fun replaceLoad(bb: Block, inst: Load): Instruction {
        return bb.update(inst) { it.copy(inst.operand()) }
    }

    private fun replaceCopy(bb: Block, inst: Copy): Instruction {
        return bb.update(inst) { it.lea(inst.origin() as Generate) }
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
                gep(generate(), nop()) (inst) -> { inst as GetElementPtr
                    when (val baseType = inst.basicType) {
                        is AggregateType -> {
                            val index = inst.index().asValue<ValueInstruction>()
                            val offset = bb.insertBefore(inst) {
                                it.arithmeticBinary(index, ArithmeticBinaryOp.Mul, Constant.of(index.type(), baseType.sizeOf()))
                            }
                            return bb.update(inst) { it.leaStack(inst.source(), Type.I8, offset) }
                        }
                        else -> {
                            baseType as PrimitiveType
                            return bb.update(inst) { it.leaStack(inst.source(), baseType, inst.index()) }
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
                                    bb.update(inst) { it.leaStack(inst.source(), Type.I8, Constant.of(Type.U32, offset)) }
                                }
                                else -> {
                                    tp as PrimitiveType
                                    bb.update(inst) { it.leaStack(inst.source(), tp, inst.index(0)) }
                                }
                            }
                        }
                        is StructType -> bb.update(inst) { it.leaStack(inst.source(), Type.U8, Constant.of(Type.U32, base.offset(inst.index(0).toInt()))) }
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
                    val move = bb.update(inst) { it.move(getSource(pointer), getIndex(pointer), inst.value()) }
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                    return move
                }
                store(gfpOrGep(generate(), nop()), nop()) (inst) -> { inst as Store
                    val pointer = inst.pointer().asValue<ValueInstruction>()
                    val st = bb.update(inst) { it.storeOnStack(getSource(pointer), getIndex(pointer), inst.value()) }
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                    return st
                }
                load(gfpOrGep(generate().not(), nop())) (inst) -> { inst as Load
                    val pointer = inst.operand().asValue<ValueInstruction>()
                    val copy = bb.update(inst) { it.indexedLoad(getSource(pointer), inst.type(), getIndex(pointer)) }
                    if (pointer.usedIn().isEmpty()) { //TODO Need DCE
                        bb.kill(pointer) // TODO bb may not contain pointer
                    }
                    return copy
                }
                load(gfpOrGep(generate(), nop())) (inst) -> { inst as Load
                    val pointer = inst.operand().asValue<ValueInstruction>()
                    val index = getIndex(pointer)
                    val copy = bb.update(inst) { it.loadFromStack(getSource(pointer), inst.type(), index) }
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
                binary(ArithmeticBinaryOp.Div, value(i8()), value(i8())) (inst) -> { inst as ArithmeticBinary
                    // Before:
                    //  %res = div i8 %a, %b
                    //
                    // After:
                    //  %extFirst  = sext %a to i16
                    //  %extSecond = sext %b to i16
                    //  %resI16 = div i16, %extFirst, %extSecond
                    //  %res = trunc i16 %resI16 to i8

                    val extFirst  = extend(bb, inst, inst.first())
                    val extSecond = extend(bb, inst, inst.second())

                    val newDiv   = bb.insertBefore(inst) { it.arithmeticBinary(extFirst, ArithmeticBinaryOp.Div, extSecond) }
                    bb.update(inst) { it.trunc(newDiv, Type.I8) }
                    return newDiv
                }
                binary(ArithmeticBinaryOp.Div, constant().not(), constant()) (inst) -> { inst as ArithmeticBinary
                    // TODO temporal
                    val second = inst.second()
                    val copy = bb.insertBefore(inst) { it.copy(second) }
                    inst.update(1, copy )
                    return inst
                }
                tupleDiv(constant().not(), constant()) (inst) -> { inst as TupleDiv
                    // TODO temporal
                    val second = inst.second()
                    val copy = bb.insertBefore(inst) { it.copy(second) }
                    inst.update(1, copy )
                    return inst
                }
                tupleDiv(value(i8()), value(i8())) (inst) -> { inst as TupleDiv
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
                store(nop(), generate()) (inst) -> { inst as Store
                    val lea = bb.insertBefore(inst) { it.lea(inst.value() as Generate) }
                    inst.update(1, lea)
                    return inst
                }
                copy(generate()) (inst) -> { inst as Copy
                    return replaceCopy(bb, inst)
                }
                ptr2int(generate()) (inst) -> { inst as Pointer2Int
                    val lea = bb.insertBefore(inst) { it.lea(inst.value() as Generate) }
                    inst.update(0, lea)
                    return inst
                }
                // TODO memcpy can be replaced with moves in some cases
                memcpy(nop(), generate(), nop()) (inst) -> { inst as Memcpy
                    val src = bb.insertBefore(inst) { it.lea(inst.source().asValue<Generate>()) }
                    inst.update(1, src)
                    return inst.prev() as Instruction //TODO refactor
                }
                memcpy(generate(), nop(), nop()) (inst) -> { inst as Memcpy
                    val dst = bb.insertBefore(inst) { it.lea(inst.destination().asValue<Generate>()) }
                    inst.update(0, dst)
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
                Lowering(fn.blocks).pass()
            }
            return module
        }
    }
}