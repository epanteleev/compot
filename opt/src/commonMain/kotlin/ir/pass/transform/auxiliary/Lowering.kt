package ir.pass.transform.auxiliary

import asm.x64.CondType
import ir.types.*
import ir.value.*
import ir.module.Module
import ir.instruction.*
import ir.value.constant.*
import ir.instruction.lir.*
import ir.global.GlobalValue
import ir.instruction.matching.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.FunctionData
import ir.module.block.Block
import ir.pass.analysis.traverse.BfsOrderOrderFabric


internal class Lowering private constructor(val cfg: FunctionData): IRInstructionVisitor<Instruction?>() {
    private var bb: Block = cfg.begin()


    private fun pass() {
        for (bb in cfg.analysis(BfsOrderOrderFabric)) {
            this.bb = bb
            bb.transform { it.accept(this) }
        }
    }

    override fun visit(alloc: Alloc): Instruction {
        // Before:
        //  %res = alloc %type
        //
        // After:
        //  %res = gen %type

        return bb.replace(alloc, Generate.gen(alloc.allocatedType))
    }

    override fun visit(generate: Generate): Instruction = generate

    override fun visit(lea: Lea): Instruction = lea

    override fun visit(add: Add): Instruction {
        return add
    }

    override fun visit(and: And): Instruction {
        return and
    }

    override fun visit(sub: Sub): Instruction? {
        return sub
    }

    override fun visit(mul: Mul): Instruction? {
        return mul
    }

    override fun visit(or: Or): Instruction {
        return or
    }

    override fun visit(xor: Xor): Instruction? {
        return xor
    }

    override fun visit(fadd: Fxor): Instruction {
        return fadd
    }

    override fun visit(shl: Shl): Instruction {
        return shl
    }

    override fun visit(shr: Shr): Instruction {
        return shr
    }

    override fun visit(div: Div): Instruction {
        return div
    }



    override fun visit(neg: Neg): Instruction {
        neg.match(neg(fp(), nop())) {
            val constant = when (neg.asType<FloatingPointType>()) {
                F32Type -> F32_SUBZERO
                F64Type -> F64_SUBZERO
            }

            return bb.replace(neg, Fxor.xor(neg.operand(), constant))
        }

        return neg
    }

    override fun visit(not: Not): Instruction {
        return not
    }

    override fun visit(branch: Branch): Instruction {
        return branch
    }

    override fun visit(branchCond: BranchCond): Instruction {
        return branchCond
    }

    override fun visit(call: Call): Instruction {
        return call
    }

    override fun visit(tupleCall: TupleCall): Instruction {
        return tupleCall
    }

    override fun visit(flag2Int: Flag2Int): Instruction {
        return flag2Int
    }

    override fun visit(bitcast: Bitcast): Instruction {
        return bitcast
    }

    override fun visit(itofp: Int2Float): Instruction? {
        return itofp
    }

    override fun visit(utofp: Unsigned2Float): Instruction {
        return utofp
    }

    override fun visit(zext: ZeroExtend): Instruction {
        return zext
    }

    override fun visit(sext: SignExtend): Instruction {
        return sext
    }

    override fun visit(trunc: Truncate): Instruction {
        return trunc
    }

    override fun visit(fptruncate: FpTruncate): Instruction {
        return fptruncate
    }

    override fun visit(fpext: FpExtend): Instruction {
        return fpext
    }

    override fun visit(fptosi: Float2Int): Instruction {
        return fptosi
    }

    override fun visit(copy: Copy): Instruction {
        copy.match(copy(generate())) {
            // Before:
            //  %res = copy %gen
            //
            // After:
            //  %lea = lea %gen
            //  %res = copy %lea

            return bb.replace(copy, Lea.lea(copy.origin().asValue()))
        }

        return copy
    }

    override fun visit(move: Move): Instruction {
        return move
    }

    override fun visit(move: MoveByIndex): Instruction {
        return move
    }

    override fun visit(downStackFrame: DownStackFrame): Instruction {
        return downStackFrame
    }

    override fun visit(gep: GetElementPtr): Instruction {
        gep.match(gep(gValue(anytype()), nop())) {
            // Before:
            //  %res = gep @global, %idx
            //
            // after:
            //  %lea = lea @global
            //  %res = gep %lea, %idx

            val lea = bb.putBefore(gep, Lea.lea(gep.source().asValue()))
            bb.updateDF(gep, GetElementPtr.SOURCE, lea)
            return lea
        }

        gep.match(gep(primitive(), generate(), nop())) {
            val baseType = gep.basicType.asType<PrimitiveType>()
            return bb.replace(gep, LeaStack.lea(gep.source(), baseType, gep.index()))
        }

        gep.match(gep(aggregate(), generate(), nop())) {
            val baseType = gep.basicType.asType<AggregateType>()
            val index = gep.index()
            val mul = Mul.mul(index, NonTrivialConstant.of(index.asType(), baseType.sizeOf()))
            val offset = bb.putBefore(gep, mul)
            return bb.replace(gep, LeaStack.lea(gep.source(), I8Type, offset))
        }

        return gep
    }

    override fun visit(gfp: GetFieldPtr): Instruction {
        gfp.match(gfp(gValue(anytype()))) {
            // Before:
            //  %res = gfp @global, %idx
            //
            // after:
            //  %lea = lea @global
            //  %res = gfp %lea, %idx

            val lea = bb.putBefore(gfp, Lea.lea(gfp.source().asValue()))
            bb.updateDF(gfp, GetFieldPtr.SOURCE, lea)
            return lea
        }

        gfp.match(gfp(aggregate(), generate())) {
            // Before:
            //  %res = gfp %gen, %idx
            //
            // After:
            //  %lea = leastv %gen, %idx

            val basicType = gfp.basicType.asType<AggregateType>()
            val index = U64Value.of(basicType.offset(gfp.index().toInt()))
            return bb.replace(gfp, LeaStack.lea(gfp.source(), U8Type, index))
        }

        return gfp
    }

    override fun visit(icmp: IntCompare): Instruction {
        icmp.match(icmp(nop(), ptr(), generate())) {
            // Before:
            //  %res = icmp %pred, %gen
            //
            // After:
            //  %lea = lea %gen
            //  %res = icmp %pred, %lea

            val lea = bb.putBefore(icmp, Lea.lea(icmp.second()))
            bb.updateDF(icmp, IntCompare.SECOND, lea)
            return lea
        }

        icmp.match(icmp(generate(), ptr(), nop())) {
            // Before:
            //  %res = icmp %pred, %gen
            //
            // After:
            //  %lea = lea %gen
            //  %res = icmp %pred, %lea

            val lea = bb.putBefore(icmp, Lea.lea(icmp.first()))
            bb.updateDF(icmp, IntCompare.FIRST, lea)
            return lea
        }

        return icmp
    }

    override fun visit(fcmp: FloatCompare): Instruction {
        return fcmp
    }

    override fun visit(load: Load): Instruction {
        load.match(load(generate(primitive()))) {
            // Before:
            //  %res = load %gen
            //
            // After:
            //  %lea = copy %gen

            return bb.replace(load, Copy.copy(load.operand()))
        }

        load.match(load(gfpOrGep(argumentByValue() or generate(), nop()))) {
            val pointer = load.operand().asValue<ValueInstruction>()
            val index = getIndex(pointer)
            val copy = bb.replace(load, LoadFromStack.load(getSource(pointer), load.type(), index))
            killOnDemand(pointer)
            return copy
        }
        load.match(load(gfpOrGep(generate().not(), nop()))) {
            val pointer = load.operand().asValue<ValueInstruction>()
            val copy = bb.replace(load, IndexedLoad.load(getSource(pointer), load.type(), getIndex(pointer)))
            killOnDemand(pointer)
            return copy
        }

        return load
    }

    override fun visit(phi: Phi): Instruction {
        return phi
    }

    override fun visit(returnValue: ReturnValue): Instruction {
        returnValue.match(ret(gValue(anytype()))) { ret: ReturnValue ->
            // Before:
            //  ret @global
            //
            // After:
            //  %lea = lea @global
            //  ret %lea

            val toValue = ret.returnValue(0)
            val lea = bb.putBefore(returnValue, Lea.lea(toValue))
            bb.updateDF(returnValue, ReturnValue.RET_VALUE, lea)
            return lea
        }

        return returnValue
    }

    override fun visit(returnVoid: ReturnVoid): Instruction {
        return returnVoid
    }

    override fun visit(indirectionCall: IndirectionCall): Instruction {
        return indirectionCall
    }

    override fun visit(indirectionVoidCall: IndirectionVoidCall): Instruction {
        return indirectionVoidCall
    }

    override fun visit(select: Select): Instruction {
        select.match(select(nop(), value(i8()), value(i8()))) {
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
                else                  -> select
            }

            val extOnTrue  = bb.putBefore(insertPos, SignExtend.sext(select.onTrue(), I16Type))
            val extOnFalse = bb.putBefore(insertPos, SignExtend.sext(select.onFalse(), I16Type))
            val newSelect  = bb.putBefore(select, Select.select(select.condition(), I16Type, extOnTrue, extOnFalse))
            return bb.replace(select, Truncate.trunc(newSelect, I8Type))
        }

        select.match(select(nop(), value(u8()), value(u8()))) {
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
                else                  -> select
            }

            val extOnTrue  = bb.putBefore(insertPos, ZeroExtend.zext(select.onTrue(), U16Type))
            val extOnFalse = bb.putBefore(insertPos, ZeroExtend.zext(select.onFalse(), U16Type))
            val newSelect  = bb.putBefore(select, Select.select(select.condition(), U16Type, extOnTrue, extOnFalse))
            return bb.replace(select, Truncate.trunc(newSelect, U8Type))
        }

        return select
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
            U64Value.of(inst.basicType.offset(index).toLong() / field.sizeOf())
        }
        else -> throw IllegalArgumentException("Expected GEP or GFP")
    }

    private fun killOnDemand(instruction: LocalValue) {
        instruction as Instruction
        if (instruction.usedIn().isEmpty()) { //TODO Need DCE
            instruction.owner().kill(instruction, UndefValue) // TODO bb may not contain pointer
        }
    }

    override fun visit(store: Store): Instruction {
        store.match(store(nop(), generate())) {
            // Before:
            //  store %ptr, %val
            //
            // After:
            //  %res = lea %ptr
            //  store %res, %val

            val lea = bb.putBefore(store, Lea.lea(store.value().asValue()))
            bb.updateDF(store, Store.VALUE, lea)
            return lea
        }
        store.match(store(gValue(primitive()), nop())) {
            // Before:
            //  %res = store i8 @global, %ptr
            //
            // After:
            //  %lea = lea @global
            //  %res = store i8 %val, %lea

            val lea = bb.putBefore(store, Lea.lea(store.pointer().asValue()))
            bb.updateDF(store, Store.DESTINATION, lea)
            return lea
        }

        store.match(store(generate(), gValue(anytype()))) {
            // Before:
            //  store %gen, @global
            //
            // After:
            //  %lea = lea @global
            //  move %gen, %lea

            val toValue = store.pointer().asValue<Generate>()
            val value = bb.putBefore(store, Lea.lea(store.value()))
            return bb.replace(store, Move.move(toValue, value))
        }

        store.match(store(gValue(primitive()), value(primitive()))) {
            // Before:
            //  store @global, %val
            //
            // After:
            //  move @global, %val

            val toValue = store.pointer().asValue<GlobalValue>()
            return bb.replace(store, Move.move(toValue, store.value()))
        }

        store.match(store(generate(primitive()), nop())) {
            // Before:
            //  store %gen, %ptr
            //
            // After:
            //  move %gen, %ptr

            val toValue = store.pointer().asValue<Generate>()
            return bb.replace(store, Move.move(toValue, store.value()))
        }

        store.match(store(gfpOrGep(argumentByValue() or generate(), nop()), nop())) {
            val pointer = store.pointer().asValue<ValueInstruction>()
            val storeOnSt = StoreOnStack.store(getSource(pointer), getIndex(pointer), store.value())
            val st = bb.replace(store, storeOnSt)
            killOnDemand(pointer)
            return st
        }
        store.match(store(gfpOrGep(generate().not(), nop()), nop())) {
            val pointer = store.pointer().asValue<ValueInstruction>()
            val moveBy = MoveByIndex.move(getSource(pointer), getIndex(pointer), store.value())
            val move = bb.replace(store, moveBy)
            killOnDemand(pointer)
            return move
        }
        store.match(store(gfpOrGep(generate(), nop()), generate())) {
            val pointer = store.pointer().asValue<ValueInstruction>()
            val st = bb.replace(store, StoreOnStack.store(getSource(pointer), getIndex(pointer), store.value()))
            killOnDemand(pointer)
            return st
        }

        return store
    }

    override fun visit(upStackFrame: UpStackFrame): Instruction {
        return upStackFrame
    }

    override fun visit(voidCall: VoidCall): Instruction {
        return voidCall
    }

    override fun visit(int2ptr: Int2Pointer): Instruction {
        return int2ptr
    }

    override fun visit(ptr2Int: Pointer2Int): Instruction {
        ptr2Int.match(ptr2int(generate())) { ptr2int: Pointer2Int ->
            // Before:
            //  %res = ptr2int %gen
            //
            // After:
            //  %lea = lea %gen
            //  %res = ptr2int %lea

            val lea = bb.putBefore(ptr2Int, Lea.lea(ptr2int.value().asValue()))
            bb.updateDF(ptr2int, Pointer2Int.SOURCE, lea)
            return lea
        }

        return ptr2Int
    }

    override fun visit(memcpy: Memcpy): Instruction {
        memcpy.match(memcpy(stackAlloc(), stackAlloc(), nop())) {
            // Before:
            //  memcpy %src, %dst
            //
            // After:
            //  %srcLea = lea %src
            //  %dstLea = lea %dst
            //  memcpy %srcLea, %dstLea

            val src = bb.putBefore(memcpy, Lea.lea(memcpy.source()))
            bb.updateDF(memcpy, Memcpy.SOURCE, src)

            val dst = bb.putBefore(memcpy, Lea.lea(memcpy.destination()))
            bb.updateDF(memcpy, Memcpy.DESTINATION, dst)
            return memcpy
        }

        memcpy.match(memcpy(nop(), stackAlloc(), nop())) {
            // Before:
            //  memcpy %src, %dst
            //
            // After:
            //  %srcLea = lea %src
            //  memcpy %srcLea, %dst

            val src = bb.putBefore(memcpy, Lea.lea(memcpy.source()))
            bb.updateDF(memcpy, Memcpy.SOURCE, src)

            val copyDst = bb.putBefore(memcpy, Copy.copy(memcpy.destination()))
            bb.updateDF(memcpy, Memcpy.DESTINATION, copyDst)
            return memcpy
        }

        memcpy.match(memcpy(stackAlloc(), nop(), nop())) {
            // Before:
            //  memcpy %src, %dst
            //
            // After:
            //  %dstLea = lea %dst
            //  memcpy %src, %dstLea

            val copySrc = bb.putBefore(memcpy, Copy.copy(memcpy.source()))
            bb.updateDF(memcpy, Memcpy.SOURCE, copySrc)

            val dst = bb.putBefore(memcpy, Lea.lea(memcpy.destination()))
            bb.updateDF(memcpy, Memcpy.DESTINATION, dst)
            return memcpy
        }

        memcpy.match(memcpy(nop(), nop(), nop())) {
            // Before:
            //  memcpy %src, %dst
            //
            // After:
            //  %srcLea = copy %src
            //  %dstLea = copy %dst
            //  memcpy %srcLea, %dstLea

            val copySrc = bb.putBefore(memcpy, Copy.copy(memcpy.source()))
            val copyDst = bb.putBefore(memcpy, Copy.copy(memcpy.destination()))
            bb.updateDF(memcpy, Memcpy.SOURCE, copySrc)
            bb.updateDF(memcpy, Memcpy.DESTINATION, copyDst)
            return memcpy
        }

        return memcpy
    }

    override fun visit(indexedLoad: IndexedLoad): Instruction {
        return indexedLoad
    }

    override fun visit(store: StoreOnStack): Instruction {
        return store
    }

    override fun visit(loadst: LoadFromStack): Instruction {
        return loadst
    }

    override fun visit(leaStack: LeaStack): Instruction {
        return leaStack
    }

    private fun truncateProjections(tupleDiv: TupleDiv, newDiv: TupleDiv, type: IntegerType): Instruction {
        //  %projDiv = proj %newDiv, 0
        //  %projRem = proj %newDiv, 1
        //  %res = trunc %projDiv to u8
        //  %rem = trunc %projRem to u8

        val divProj = tupleDiv.quotient()
        val quotient = bb.putBefore(tupleDiv, Projection.proj(newDiv, 0))
        val quotientTrunc = bb.putBefore(tupleDiv, Truncate.trunc(quotient, type))
        if (divProj != null) {
            bb.updateUsages(divProj) { quotientTrunc }
            killOnDemand(divProj)
        }

        val remainder = tupleDiv.remainder()
        val proj      = bb.putBefore(tupleDiv, Projection.proj(newDiv, 1))
        val remainderTruncate = bb.putBefore(tupleDiv, Truncate.trunc(proj, type))
        if (remainder != null) {
            bb.updateUsages(remainder) { remainderTruncate }
            killOnDemand(remainder)
        }
        killOnDemand(tupleDiv)
        return remainderTruncate
    }

    override fun visit(tupleDiv: TupleDiv): Instruction {
        tupleDiv.match(tupleDiv(value(i8()), value(i8()))) {
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

            val extFirst  = bb.putBefore(tupleDiv, SignExtend.sext(tupleDiv.first(), I16Type))
            val extSecond = bb.putBefore(tupleDiv, SignExtend.sext(tupleDiv.second(), I16Type))
            val newDiv    = bb.putBefore(tupleDiv, TupleDiv.div(extFirst, extSecond))

            return truncateProjections(tupleDiv, newDiv, I8Type)
        }
        tupleDiv.match(tupleDiv(value(u8()), value(u8()))) {
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

            val extFirst  = bb.putBefore(tupleDiv, ZeroExtend.zext(tupleDiv.first(), U16Type))
            val extSecond = bb.putBefore(tupleDiv, ZeroExtend.zext(tupleDiv.second(), U16Type))
            val newDiv    = bb.putBefore(tupleDiv, TupleDiv.div(extFirst, extSecond))

            return truncateProjections(tupleDiv, newDiv, U8Type)
        }
        tupleDiv.match(tupleDiv(constant().not(), nop())) {
            // TODO temporal
            val second = tupleDiv.second()
            val copy = bb.putBefore(tupleDiv, Copy.copy(second))
            bb.updateDF(tupleDiv, TupleDiv.SECOND, copy)
            return tupleDiv
        }

        return tupleDiv
    }

    override fun visit(proj: Projection): Instruction {
        return proj
    }

    override fun visit(switch: Switch): Instruction {
        return switch
    }

    override fun visit(tupleCall: IndirectionTupleCall): Instruction {
        return tupleCall
    }

    override fun visit(intrinsic: Intrinsic): Instruction {
        return intrinsic
    }

    companion object {
        private val F32_SUBZERO = F32Value(-0.0f)
        private val F64_SUBZERO = F64Value(-0.0)

        fun run(module: Module): Module {
            for (fn in module.functions()) {
                Lowering(fn).pass()
            }

            return module
        }
    }
}