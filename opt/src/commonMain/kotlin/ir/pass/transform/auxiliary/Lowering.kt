package ir.pass.transform.auxiliary

import ir.types.*
import ir.value.*
import ir.module.*
import ir.value.constant.*
import ir.instruction.*
import ir.module.block.Block
import ir.global.GlobalValue
import ir.instruction.lir.Generate
import ir.instruction.matching.*
import ir.pass.analysis.traverse.BfsOrderOrderFabric


class Lowering private constructor(private val cfg: FunctionData) {
    private fun replaceGepToLea() {
        fun transform(bb: Block, inst: Instruction): Instruction {
            inst.match(gep(primitive(), generate(), nop())) { gp: GetElementPtr ->
                val baseType = gp.basicType.asType<PrimitiveType>()
                return bb.replace(inst) { it.leaStack(gp.source(), baseType, gp.index()) }
            }

            inst.match(gep(aggregate(), generate(), nop())) { gp: GetElementPtr ->
                val baseType = gp.basicType.asType<AggregateType>()
                val index = gp.index()
                val offset = bb.insertBefore(inst) {
                    it.mul(index, NonTrivialConstant.of(index.asType(), baseType.sizeOf()))
                }
                return bb.replace(inst) { it.leaStack(gp.source(), I8Type, offset) }
            }

            inst.match(gfp(struct(), generate())) { gf: GetFieldPtr ->
                val basicType = gf.basicType.asType<StructType>()
                val index = U64Value(basicType.offset(gf.index().toInt()))
                return bb.replace(inst) { it.leaStack(gf.source(), U8Type, index) }
            }

            inst.match(gfp(array(primitive()), generate())) { gf: GetFieldPtr ->
                val baseType = gf.basicType.asType<ArrayType>().elementType().asType<PrimitiveType>()
                return bb.replace(inst) { it.leaStack(gf.source(), baseType, gf.index()) }
            }

            inst.match(gfp(array(aggregate()), generate())) { gf: GetFieldPtr ->
                val tp = gf.basicType.asType<ArrayType>()
                val index = gf.index().toInt()
                val offset = tp.offset(index)
                return bb.replace(inst) { it.leaStack(gf.source(), I8Type, U32Value(offset)) }
            }

            return inst
        }

        for (bb in cfg) {
            bb.transform { inst -> transform(bb, inst) }
        }
    }

    private fun getSource(inst: Instruction): Value = when (inst) {
        is GetElementPtr -> inst.source()
        is GetFieldPtr   -> inst.source()
        else             -> throw IllegalArgumentException("Expected GEP or GFP")
    }

    private fun getIndex(inst: Instruction): Value = when (inst) {
        is GetElementPtr -> inst.index()
        is GetFieldPtr -> {
            val index = inst.index().toInt()
            val field = inst.basicType.field(index)
            U64Value(inst.basicType.offset(index).toLong() / field.sizeOf())
        }
        else -> throw IllegalArgumentException("Expected GEP or GFP")
    }

    private fun replaceGEPAndStore() {
        fun closure(bb: Block, inst: Instruction): Instruction {
            inst.match(store(gfpOrGep(argumentByValue(), nop()), nop())) { store: Store ->
                val pointer = store.pointer().asValue<ValueInstruction>()
                val st = bb.replace(inst) { it.storeOnStack(getSource(pointer), getIndex(pointer), store.value()) }
                killOnDemand(pointer.owner(), pointer)
                return st
            }
            inst.match(store(gfpOrGep(generate().not(), nop()), generate())) { store: Store ->
                val pointer = store.pointer().asValue<ValueInstruction>()
                val move = bb.replace(inst) { it.move(getSource(pointer), getIndex(pointer), store.value()) }
                killOnDemand(bb, pointer)
                return move
            }
            inst.match(store(gfpOrGep(generate(), nop()), generate())) { store: Store ->
                val pointer = store.pointer().asValue<ValueInstruction>()
                val st = bb.replace(inst) { it.storeOnStack(getSource(pointer), getIndex(pointer), store.value()) }
                killOnDemand(bb, pointer)
                return st
            }
            inst.match(load(gfpOrGep(argumentByValue(), nop()))) { load: Load ->
                val pointer = load.operand().asValue<ValueInstruction>()
                val index = getIndex(pointer)
                val copy = bb.replace(inst) { it.loadFromStack(getSource(pointer), load.type(), index) }
                killOnDemand(pointer.owner(), pointer)
                return copy
            }
            inst.match(load(gfpOrGep(generate().not(), nop()))) { load: Load ->
                val pointer = load.operand().asValue<ValueInstruction>()
                val copy = bb.replace(inst) { it.indexedLoad(getSource(pointer), load.type(), getIndex(pointer)) }
                killOnDemand(bb, pointer)
                return copy
            }
            inst.match(load(gfpOrGep(generate(), nop()))) { load: Load ->
                val pointer = load.operand().asValue<ValueInstruction>()
                val index = getIndex(pointer)
                val copy = bb.replace(inst) { it.loadFromStack(getSource(pointer), load.type(), index) }
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
            bb.kill(instruction, UndefValue) // TODO bb may not contain pointer
        }
    }

    private fun replaceByteOperands() {
        fun closure(bb: Block, inst: Instruction): Instruction {
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

                val extFirst  = bb.insertBefore(inst) { it.sext(tupleDiv.first(), I16Type) }
                val extSecond = bb.insertBefore(inst) { it.sext(tupleDiv.second(), I16Type) }
                val newDiv    = bb.insertBefore(inst) { it.tupleDiv(extFirst, extSecond) }


                val divProj = tupleDiv.quotient()
                if (divProj != null) {
                    val proj = bb.insertBefore(inst) { it.proj(newDiv, 0) }
                    bb.updateUsages(divProj) { bb.insertBefore(inst) { it.trunc(proj, I8Type) } }
                    killOnDemand(bb, divProj)
                }

                val remProj  = tupleDiv.remainder() ?: throw IllegalStateException("Remainder projection is missing")
                val proj     = bb.insertBefore(inst) { it.proj(newDiv, 1) }
                val truncate = bb.updateUsages(remProj) {
                    bb.insertBefore(inst) { it.trunc(proj, I8Type) }
                }
                killOnDemand(bb, remProj)

                killOnDemand(bb, tupleDiv)
                return truncate
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

                val extFirst  = bb.insertBefore(inst) { it.zext(tupleDiv.first(), U16Type) }
                val extSecond = bb.insertBefore(inst) { it.zext(tupleDiv.second(), U16Type) }
                val newDiv    = bb.insertBefore(inst) { it.tupleDiv(extFirst, extSecond) }

                val divProj = tupleDiv.proj(0)
                if (divProj != null) {
                    val proj = bb.insertBefore(inst) { it.proj(newDiv, 0) }
                    bb.updateUsages(divProj) { bb.insertBefore(inst) { it.trunc(proj, U8Type) } }
                    killOnDemand(bb, divProj)
                }

                val remProj  = tupleDiv.remainder() ?: throw IllegalStateException("Remainder projection is missing")
                val proj     = bb.insertBefore(inst) { it.proj(newDiv, 1) }
                val truncate = bb.updateUsages(remProj) {
                    bb.insertBefore(inst) { it.trunc(proj, U8Type) }
                }
                killOnDemand(bb, remProj)
                killOnDemand(bb, tupleDiv)
                return truncate
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

                val insertPos = when(val selectCond = select.condition()) {
                    is CompareInstruction -> selectCond
                    else                  -> inst
                }

                val extOnTrue  = bb.insertBefore(insertPos) { it.sext(select.onTrue(), I16Type) }
                val extOnFalse = bb.insertBefore(insertPos) { it.sext(select.onFalse(), I16Type) }
                val newSelect  = bb.insertBefore(inst) { it.select(select.condition(), I16Type, extOnTrue, extOnFalse) }
                return bb.replace(inst) { it.trunc(newSelect, I8Type) }
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

                val insertPos = when(val selectCond = select.condition()) {
                    is CompareInstruction -> selectCond
                    else                  -> inst
                }

                val extOnTrue  = bb.insertBefore(insertPos) { it.zext(select.onTrue(), U16Type) }
                val extOnFalse = bb.insertBefore(insertPos) { it.zext(select.onFalse(), U16Type) }
                val newSelect  = bb.insertBefore(inst) { it.select(select.condition(), U16Type, extOnTrue, extOnFalse) }
                return bb.replace(inst) { it.trunc(newSelect, U8Type) }
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
                // Before:
                //  %res = alloc %type
                //
                // After:
                //  %res = gen %type

                return bb.replace(inst) { it.gen(alloc.allocatedType) }
            }
            inst.match(store(nop(), generate())) { store: Store ->
                // Before:
                //  store %ptr, %val
                //
                // After:
                //  %res = lea %ptr
                //  store %res, %val

                val lea = bb.insertBefore(inst) { it.lea(store.value().asValue()) }
                bb.updateDF(inst, Store.VALUE, lea)
                return lea
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
                // Before:
                //  %res = copy %gen
                //
                // After:
                //  %lea = lea %gen
                //  %res = copy %lea

                return bb.replace(inst) { it.lea(copy.origin().asValue()) }
            }
            inst.match(ptr2int(generate())) { ptr2int: Pointer2Int ->
                // Before:
                //  %res = ptr2int %gen
                //
                // After:
                //  %lea = lea %gen
                //  %res = ptr2int %lea

                val lea = bb.insertBefore(inst) { it.lea(ptr2int.value().asValue()) }
                bb.updateDF(inst, Pointer2Int.SOURCE, lea)
                return lea
            }
            inst.match(memcpy(nop(), generate(), nop())) { memcpy: Memcpy ->
                // Before:
                //  memcpy %src, %dst
                //
                // After:
                //  %srcLea = lea %src
                //  memcpy %srcLea, %dst

                val src = bb.insertBefore(inst) { it.lea(memcpy.source().asValue<Generate>()) }
                bb.updateDF(inst, Memcpy.SOURCE, src)
                return bb.idom(inst)
            }
            inst.match(memcpy(generate(), nop(), nop())) { memcpy: Memcpy ->
                // Before:
                //  memcpy %src, %dst
                //
                // After:
                //  %dstLea = lea %dst
                //  memcpy %src, %dstLea

                val dst = bb.insertBefore(inst) { it.lea(memcpy.destination().asValue<Generate>()) }
                bb.updateDF(inst, Memcpy.DESTINATION, dst)
                return bb.idom(inst)
            }

            inst.match(icmp(nop(), ptr(), generate())) { icmp: IntCompare ->
                // Before:
                //  %res = icmp %pred, %gen
                //
                // After:
                //  %lea = lea %gen
                //  %res = icmp %pred, %lea

                val lea = bb.insertBefore(inst) { it.lea(icmp.second()) }
                bb.updateDF(inst, IntCompare.SECOND, lea)
                return lea
            }

            inst.match(icmp(generate(), ptr(), nop())) { icmp: IntCompare ->
                // Before:
                //  %res = icmp %pred, %gen
                //
                // After:
                //  %lea = lea %gen
                //  %res = icmp %pred, %lea

                val lea = bb.insertBefore(inst) { it.lea(icmp.first()) }
                bb.updateDF(inst, IntCompare.FIRST, lea)
                return lea
            }

            inst.match(store(generate(), gValue(anytype()))) { store: Store ->
                // Before:
                //  store %gen, @global
                //
                // After:
                //  %lea = lea @global
                //  move %gen, %lea

                val toValue = store.pointer().asValue<Generate>()
                val value = bb.insertBefore(store) { it.lea(store.value()) }
                return bb.replace(store) { it.move(toValue, value) }
            }

            inst.match(store(gValue(primitive()), value(primitive()))) { store: Store ->
                // Before:
                //  store @global, %val
                //
                // After:
                //  move @global, %val

                val toValue = store.pointer().asValue<GlobalValue>()
                return bb.replace(store) { it.move(toValue, store.value()) }
            }

            inst.match(store(generate(primitive()), nop())) { store: Store ->
                // Before:
                //  store %gen, %ptr
                //
                // After:
                //  move %gen, %ptr

                val toValue = store.pointer().asValue<Generate>()
                return bb.replace(store) { it.move(toValue, store.value()) }
            }

            inst.match(load(generate(primitive()))) { load: Load ->
                // Before:
                //  %res = load %gen
                //
                // After:
                //  %lea = copy %gen

                return bb.replace(load) { it.copy(load.operand()) }
            }

            return inst
        }

        for (bb in cfg.analysis(BfsOrderOrderFabric)) {
            bb.transform { inst -> closure(bb, inst) }
        }
    }

    private fun pass() {
        replaceByteOperands()
        replaceEscaped()
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