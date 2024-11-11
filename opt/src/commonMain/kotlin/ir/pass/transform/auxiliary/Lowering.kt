package ir.pass.transform.auxiliary

import ir.types.*
import ir.value.*
import ir.module.*
import ir.value.constant.*
import ir.instruction.*
import ir.module.block.Block
import ir.global.GlobalValue
import ir.instruction.matching.*
import ir.pass.analysis.traverse.BfsOrderOrderFabric
import ir.value.constant.Constant


class Lowering private constructor(private val cfg: FunctionData) {
    private fun replaceAllocLoadStores() {
        fun transform(bb: Block, inst: Instruction): Instruction? {
            inst.match(store(generate(), gValue(anytype()))) { store: Store ->
                val toValue = store.pointer().asValue<Generate>()
                val value = bb.insertBefore(store) { it.lea(store.value()) }
                return bb.replace(store) { it.move(toValue, value) }
            }

            inst.match(store(gValue(primitive()), value(primitive()))) { store: Store ->
                val toValue = store.pointer().asValue<GlobalValue>()
                return bb.replace(store) { it.move(toValue, store.value()) }
            }

            inst.match(store(generate(primitive()), nop())) { store: Store ->
                val toValue = store.pointer().asValue<Generate>()
                return bb.replace(store) { it.move(toValue, store.value()) }
            }
            inst.match(load(generate(primitive()))) { load: Load ->
                val fromValue = load.operand().asValue<Generate>()
                return bb.replace(load) { it.copy(fromValue) }
            }

            return inst
        }

        for (bb in cfg) {
            bb.transform { inst -> transform(bb, inst) }
        }
    }

    private fun replaceGepToLea() {
        fun transform(bb: Block, inst: Instruction): Instruction? = match(inst) {
            gep(primitive(), generate(), nop()) { gp ->
                val baseType = gp.basicType.asType<PrimitiveType>()
                bb.replace(inst) { it.leaStack(gp.source(), baseType, gp.index()) }
            }
            gep(aggregate(), generate(), nop()) { gp ->
                val baseType = gp.basicType.asType<AggregateType>()
                val index = gp.index()
                val offset = bb.insertBefore(inst) {
                    it.mul(index, Constant.of(index.asType(), baseType.sizeOf()))
                }
                bb.replace(inst) { it.leaStack(gp.source(), Type.I8, offset) }
            }
            gfp(primitive(), generate()) { gf ->
                val baseType = gf.basicType.asType<PrimitiveType>()
                bb.replace(inst) { it.leaStack(gf.source(), baseType, gf.index(0)) }
            }
            gfp(struct(), generate()) { gf ->
                val basicType = gf.basicType.asType<StructType>()
                val index = Constant.of(Type.U32, basicType.offset(gf.index(0).toInt()))
                bb.replace(inst) { it.leaStack(gf.source(), Type.U8, index) }
            }
            gfp(array(primitive()), generate()) { gf ->
                val baseType = gf.basicType.asType<ArrayType>().elementType().asType<PrimitiveType>()
                bb.replace(inst) { it.leaStack(gf.source(), baseType, gf.index(0)) }
            }
            gfp(array(aggregate()), generate()) { gf ->
                val tp = gf.basicType.asType<ArrayType>()
                val index = gf.index(0).toInt()
                val offset = tp.offset(index)
                bb.replace(inst) { it.leaStack(gf.source(), Type.I8, Constant.of(Type.U32, offset)) }
            }
            default()
        }

        for (bb in cfg) {
            bb.transform { inst -> transform(bb, inst) }
        }
    }

    private fun replaceGEPAndStore() {
        fun getSource(inst: Instruction): Value = when (inst) {
            is GetElementPtr -> inst.source()
            is GetFieldPtr   -> inst.source()
            else             -> throw IllegalArgumentException("Expected GEP or GFP")
        }

        fun getIndex(inst: Instruction): Value = when (inst) {
            is GetElementPtr -> inst.index()
            is GetFieldPtr -> {
                val index = inst.index(0).toInt()
                val field = inst.basicType.field(index)
                U64Value(inst.basicType.offset(index).toLong() / field.sizeOf())
            }
            else -> throw IllegalArgumentException("Expected GEP or GFP")
        }

        fun closure(bb: Block, inst: Instruction): Instruction? {
            inst.match(store(gfpOrGep(argumentByValue(), nop()), nop())) { inst: Store ->
                val pointer = inst.pointer().asValue<ValueInstruction>()
                val st = bb.replace(inst) { it.storeOnStack(getSource(pointer), getIndex(pointer), inst.value()) }
                killOnDemand(pointer.owner(), pointer)
                return st
            }
            inst.match(store(gfpOrGep(generate().not(), nop()), generate())) { inst: Store ->
                val pointer = inst.pointer().asValue<ValueInstruction>()
                val move = bb.replace(inst) { it.move(getSource(pointer), getIndex(pointer), inst.value()) }
                killOnDemand(bb, pointer)
                return move
            }
            inst.match(store(gfpOrGep(generate(), nop()), generate())) { inst: Store ->
                val pointer = inst.pointer().asValue<ValueInstruction>()
                val st = bb.replace(inst) { it.storeOnStack(getSource(pointer), getIndex(pointer), inst.value()) }
                killOnDemand(bb, pointer)
                return st
            }
            inst.match(load(gfpOrGep(argumentByValue(), nop()))) { inst: Load ->
                val pointer = inst.operand().asValue<ValueInstruction>()
                val index = getIndex(pointer)
                val copy = bb.replace(inst) { it.loadFromStack(getSource(pointer), inst.type(), index) }
                killOnDemand(pointer.owner(), pointer)
                return copy
            }
            inst.match(load(gfpOrGep(generate().not(), nop()))) { inst: Load ->
                val pointer = inst.operand().asValue<ValueInstruction>()
                val copy = bb.replace(inst) { it.indexedLoad(getSource(pointer), inst.type(), getIndex(pointer)) }
                killOnDemand(bb, pointer)
                return copy
            }
            inst.match(load(gfpOrGep(generate(), nop()))) { inst: Load ->
                val pointer = inst.operand().asValue<ValueInstruction>()
                val index = getIndex(pointer)
                val copy = bb.replace(inst) { it.loadFromStack(getSource(pointer), inst.type(), index) }
                killOnDemand(pointer.owner(), pointer)
                return copy
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

    private fun replaceByteArguments() {
        fun closure(bb: Block, inst: Instruction): Instruction? {
            inst.match(tupleDiv(value(i8()), value(i8()))) { tupleDiv: TupleDiv ->
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

                val extFirst  = bb.insertBefore(inst) { it.sext(tupleDiv .first(), Type.I16) }
                val extSecond = bb.insertBefore(inst) { it.sext(tupleDiv.second(), Type.I16) }
                val newDiv    = bb.insertBefore(inst) { it.tupleDiv(extFirst, extSecond) }
                var last: Instruction = newDiv

                val divProj = tupleDiv.quotient()
                if (divProj != null) {
                    val proj     = bb.insertBefore(inst) { it.proj(newDiv, 0) }
                    val truncate = bb.updateUsages(divProj) {
                        bb.insertBefore(inst) { it.trunc(proj, Type.I8) }
                    }
                    killOnDemand(bb, divProj)
                    last = truncate
                }

                val remProj  = tupleDiv.remainder() ?: throw IllegalStateException("Remainder projection is missing")
                val proj     = bb.insertBefore(inst) { it.proj(newDiv, 1) }
                val truncate = bb.updateUsages(remProj) {
                    bb.insertBefore(inst) { it.trunc(proj, Type.I8) }
                }
                killOnDemand(bb, remProj)
                last = truncate

                killOnDemand(bb, tupleDiv)
                return last
            }
            inst.match(tupleDiv(value(u8()), value(u8()))) { tupleDiv: TupleDiv ->
                // Before:
                //  %resANDrem = div u8 %a, %b
                //
                // After:
                //  %extFirst  = zext %a to u16
                //  %extSecond = zext %b to u16
                //  %newDiv = div u16 %extFirst, %extSecond
                //  %projDiv = proj %newDiv, 0
                //  %projRem = proj %newDiv, 1
                //  %res = trunc %projDiv to u8
                //  %rem = trunc %projRem to u8

                val extFirst  = bb.insertBefore(inst) { it.zext(tupleDiv.first(), Type.U16) }
                val extSecond = bb.insertBefore(inst) { it.zext(tupleDiv.second(), Type.U16) }
                val newDiv    = bb.insertBefore(inst) { it.tupleDiv(extFirst, extSecond) }
                var last: Instruction = newDiv

                val divProj = tupleDiv.proj(0)
                if (divProj != null) {
                    val proj     = bb.insertBefore(inst) { it.proj(newDiv, 0) }
                    val truncate = bb.updateUsages(divProj) {
                        bb.insertBefore(inst) { it.trunc(proj, Type.U8) }
                    }
                    killOnDemand(bb, divProj)
                    last = truncate
                }

                val remProj = tupleDiv.remainder() ?: throw IllegalStateException("Remainder projection is missing")
                val proj     = bb.insertBefore(inst) { it.proj(newDiv, 1) }
                val truncate = bb.updateUsages(remProj) {
                    bb.insertBefore(inst) { it.trunc(proj, Type.U8) }
                }
                killOnDemand(bb, remProj)
                last = truncate

                killOnDemand(bb, tupleDiv)
                return last
            }
            inst.match(tupleDiv(constant().not(), nop())) { tupleDiv: TupleDiv ->
                // TODO temporal
                val second = tupleDiv.second()
                val copy = bb.insertBefore(inst) { it.copy(second) }
                bb.updateDF(inst, TupleDiv.SECOND, copy)
                return inst
            }
            inst.match(select(nop(), value(i8()), value(i8()))) { select: Select ->
                // Before:
                //  %cond = icmp <predicate> i8 %a, %b
                //  %res = select i1 %cond, i8 %onTrue, i8 %onFalse
                //
                // After:
                //  %extOnTrue  = sext %onTrue to i16
                //  %extOnFalse = sext %onFalse to i16
                //  %cond = icmp <predicate> i8 %a, %b
                //  %newSelect = select i1 %cond, i16 %extOnTrue, i16 %extOnFalse
                //  %res = trunc %newSelect to i8

                val insertPos = if (select.condition() is CompareInstruction) {
                    select.condition() as CompareInstruction
                } else {
                    inst
                }

                val extOnTrue  = bb.insertBefore(insertPos) { it.sext(select.onTrue(), Type.I16) }
                val extOnFalse = bb.insertBefore(insertPos) { it.sext(select.onFalse(), Type.I16) }
                val newSelect  = bb.insertBefore(inst) { it.select(select.condition(), Type.I16, extOnTrue, extOnFalse) }
                return bb.replace(inst) { it.trunc(newSelect, Type.I8) }
            }
            inst.match(select(nop(), value(u8()), value(u8()))) { select: Select ->
                // Before:
                //  %cond = icmp <predicate> u8 %a, %b
                //  %res = select i1 %cond, u8 %onTrue, u8 %onFalse
                //
                // After:
                //  %extOnTrue  = zext %onTrue to u16
                //  %extOnFalse = zext %onFalse to u16
                //  %cond = icmp <predicate> u8 %a, %b
                //  %newSelect = select i1 %cond, u16 %extOnTrue, u16 %extOnFalse
                //  %res = trunc %newSelect to u8

                val insertPos = if (select.condition() is CompareInstruction) {
                    select.condition() as CompareInstruction
                } else {
                    inst
                }

                val extOnTrue  = bb.insertBefore(insertPos) { it.zext(select.onTrue(), Type.U16) }
                val extOnFalse = bb.insertBefore(insertPos) { it.zext(select.onFalse(), Type.U16) }
                val newSelect  = bb.insertBefore(inst) { it.select(select.condition(), Type.U16, extOnTrue, extOnFalse) }
                return bb.replace(inst) { it.trunc(newSelect, Type.U8) }
            }
            return inst
        }

        for (bb in cfg) {
            bb.transform { inst -> closure(bb, inst) }
        }
    }

    private fun replaceEscaped() {
        fun closure(bb: Block, inst: Instruction): Instruction? {
            inst.match(alloc()) { alloc: Alloc ->
                return bb.replace(inst) { it.gen(alloc.allocatedType) }
            }
            inst.match(store(nop(), generate())) { store: Store ->
                val lea = bb.insertBefore(inst) { it.lea(store.value().asValue()) }
                bb.updateDF(inst, Store.VALUE, lea)
                return inst
            }
            inst.match(store(gValue(primitive()), nop())) { store: Store ->
                // Before:
                //  %res = store i8 @global, %ptr
                //
                // After:
                //  %lea = lea @global
                //  %res = store i8 %val, %lea

                val lea = bb.insertBefore(inst) { it.lea(store.pointer().asValue()) }
                bb.updateDF(inst, Store.DESTINATION, lea)
                return lea
            }
            inst.match(gfp(gValue(anytype()))) { gfp: GetFieldPtr ->
                // Before:
                //  %res = gfp @global, %idx
                //
                // after:
                //  %lea = lea @global
                //  %res = gfp %lea, %idx

                val lea = bb.insertBefore(inst) { it.lea(gfp.source().asValue()) }
                bb.updateDF(inst, GetFieldPtr.SOURCE, lea)
                return lea
            }
            inst.match(gep(gValue(anytype()), nop())) { gep: GetElementPtr ->
                // Before:
                //  %res = gep @global, %idx
                //
                // after:
                //  %lea = lea @global
                //  %res = gep %lea, %idx

                val lea = bb.insertBefore(inst) { it.lea(gep.source().asValue()) }
                bb.updateDF(inst, GetElementPtr.SOURCE, lea)
                return lea
            }
            inst.match(copy(generate())) { copy: Copy ->
                return bb.replace(inst) { it.lea(copy.origin().asValue()) }
            }
            inst.match(ptr2int(generate())) { ptr2int: Pointer2Int ->
                val lea = bb.insertBefore(inst) { it.lea(ptr2int.value().asValue()) }
                bb.updateDF(inst, Pointer2Int.SOURCE, lea)
                return inst
            }
            inst.match(memcpy(nop(), generate(), nop())) { memcpy: Memcpy ->
                val src = bb.insertBefore(inst) { it.lea(memcpy.source().asValue<Generate>()) }
                bb.updateDF(inst, Memcpy.SOURCE, src)
                return bb.idom(inst)
            }
            inst.match(memcpy(generate(), nop(), nop())) { memcpy: Memcpy ->
                val dst = bb.insertBefore(inst) { it.lea(memcpy.destination().asValue<Generate>()) }
                bb.updateDF(inst, Memcpy.DESTINATION, dst)
                return bb.idom(inst)
            }
            return inst
        }

        for (bb in cfg.analysis(BfsOrderOrderFabric)) {
            bb.transform { inst -> closure(bb, inst) }
        }
    }

    private fun pass() {
        replaceByteArguments()
        replaceEscaped()
        replaceAllocLoadStores()
        replaceGEPAndStore()
        replaceGepToLea()
    }

    companion object {
        fun run(module: Module): Module {
            for (fn in module.functions()) {
                Lowering(fn).pass()
            }
            return module
        }
    }
}