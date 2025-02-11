package ir.module.auxiliary

import common.arrayFrom
import common.assertion
import common.forEachWith
import common.intMapOf
import ir.attributes.FunctionAttribute
import ir.global.GlobalSymbol
import ir.instruction.*
import ir.instruction.Copy
import ir.instruction.lir.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.FunctionData
import ir.module.block.Block
import ir.pass.analysis.traverse.PreOrderFabric
import ir.value.*
import ir.value.constant.Constant


class CopyCFG private constructor(private val fd: FunctionData) : IRInstructionVisitor<InstBuilder<Instruction>>() {
    private val oldValuesToNew = hashMapOf<LocalValue, LocalValue>()
    private val newCFG         = FunctionData.create(fd.prototype, copyArguments())
    private val oldToNewBlock  = setupNewBasicBlock()
    private var currentBB: Block? = null

    private val bb: Block
        get() = currentBB?: throw RuntimeException("currentBB is null")

    private fun setupNewBasicBlock(): Map<Block, Block> {
        val oldToNew = intMapOf<Block, Block>(fd.size()) { it.index }

        for (old in fd) {
            if (old.index == 0) { //TODO
                oldToNew[old] = newCFG.begin()
                continue
            }
            oldToNew[old] = newCFG.blocks.createBlock()
        }

        return oldToNew
    }

    private fun copyArguments(): List<ArgumentValue> {
        val newArgs = arrayListOf<ArgumentValue>()
        fd.arguments().forEachWith(fd.prototype.arguments()) { arg, type, i ->
            val newArg = ArgumentValue(i, type, arg.attributes)
            oldValuesToNew[arg] = newArg
            newArgs.add(newArg)
        }
        return newArgs
    }

    fun copy(): FunctionData {
        copyBasicBlocks()
        return newCFG
    }

    private fun copyBasicBlocks() {
        for (bb in fd.analysis(PreOrderFabric)) {
            copyBasicBlocks(bb)
        }

        updatePhis()
    }

    private fun copyBasicBlocks(thisBlock: Block) {
        currentBB = oldToNewBlock[thisBlock]!!
        for (inst in thisBlock) {
            newInst(inst)
        }
    }

    private fun newInst(inst: Instruction): LocalValue? {
        val builder = inst.accept(this)
        val newInstruction = bb.put(builder)
        if (inst is LocalValue) {
            oldValuesToNew[inst] = newInstruction as LocalValue
            return newInstruction
        } else {
            return null
        }
    }

    private fun updatePhis() {
        for (bb in newCFG) {
            bb.phis { phi ->
                bb.updateDF(phi) { _, value -> mapUsage(value) }
            }
        }
    }

    private inline fun<reified T> mapUsage(old: Value): T {
        if (old is Constant || old is GlobalSymbol) {
            return old as T
        }
        val value = oldValuesToNew[old]?: let {
            throw RuntimeException("cannot find localValue=${old}")
        }
        if (value !is T) {
            throw RuntimeException("unexpected type for value=$value")
        }

        return value
    }

    private fun mapOperands(old: Instruction): List<Value> {
        assertion(old !is Callable) { "unexpected type for instruction=$old" }

        val newUsages = arrayListOf<Value>()
        old.operands {
            newUsages.add(mapUsage<Value>(it))
        }

        return newUsages
    }

    private fun mapArguments(old: Callable): List<Value> {
        val newUsages = arrayListOf<Value>()
        old.arguments().forEach {
            newUsages.add(mapUsage<Value>(it))
        }

        return newUsages
    }

    private fun mapBlock(old: Block): Block {
        return oldToNewBlock[old]?: throw RuntimeException("cannot find new block, oldBlock=$old")
    }

    private fun cloneAttributes(call: Callable): Set<FunctionAttribute> {
        return call.attributes().mapTo(hashSetOf()) { it }
    }

    override fun visit(alloc: Alloc): InstBuilder<Instruction> {
        return Alloc.alloc(alloc.allocatedType)
    }

    override fun visit(generate: Generate): InstBuilder<Instruction> {
        return Generate.gen(generate.type())
    }

    override fun visit(lea: Lea): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(lea.operand())
        return Lea.lea(operand)
    }

    override fun visit(add: Add): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(add.lhs())
        val second = mapUsage<Value>(add.rhs())

        return Add.add(first, second)
    }

    override fun visit(and: And): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(and.lhs())
        val second = mapUsage<Value>(and.rhs())

        return And.and(first, second)
    }

    override fun visit(div: Div): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(div.lhs())
        val second = mapUsage<Value>(div.rhs())

        return Div.div(first, second)
    }

    override fun visit(mul: Mul): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(mul.lhs())
        val second = mapUsage<Value>(mul.rhs())

        return Mul.mul(first, second)
    }

    override fun visit(or: Or): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(or.lhs())
        val second = mapUsage<Value>(or.rhs())

        return Or.or(first, second)
    }

    override fun visit(shl: Shl): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(shl.lhs())
        val second = mapUsage<Value>(shl.rhs())

        return Shl.shl(first, second)
    }

    override fun visit(shr: Shr): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(shr.lhs())
        val second = mapUsage<Value>(shr.rhs())

        return Shr.shr(first, second)
    }

    override fun visit(sub: Sub): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(sub.lhs())
        val second = mapUsage<Value>(sub.rhs())

        return Sub.sub(first, second)
    }

    override fun visit(xor: Xor): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(xor.lhs())
        val second = mapUsage<Value>(xor.rhs())

        return Xor.xor(first, second)
    }

    override fun visit(fadd: Fxor): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(fadd.lhs())
        val second = mapUsage<Value>(fadd.rhs())

        return Fxor.xor(first, second)
    }

    override fun visit(neg: Neg): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(neg.operand())
        return Neg.neg(operand)
    }

    override fun visit(not: Not): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(not.operand())
        return Not.not(operand)
    }

    override fun visit(branch: Branch): InstBuilder<Instruction> {
        return Branch.br(mapBlock(branch.target()))
    }

    override fun visit(branchCond: BranchCond): InstBuilder<Instruction> {
        val condition = mapUsage<Value>(branchCond.condition())
        val onTrue    = mapBlock(branchCond.onTrue())
        val onFalse   = mapBlock(branchCond.onFalse())
        return BranchCond.br(condition, onTrue, onFalse)
    }

    override fun visit(call: Call): InstBuilder<Instruction> {
        val newUsages = mapArguments(call)
        val target    = mapBlock(call.target())

        return Call.call(call.prototype(), newUsages, cloneAttributes(call), target)
    }

    override fun visit(tupleCall: TupleCall): InstBuilder<Instruction> {
        val newUsages = mapArguments(tupleCall)
        val target    = mapBlock(tupleCall.target())

        return TupleCall.call(tupleCall.prototype(), newUsages, cloneAttributes(tupleCall), target)
    }

    override fun visit(bitcast: Bitcast): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(bitcast.value())
        return Bitcast.bitcast(operand, bitcast.type())
    }

    override fun visit(flag2Int: Flag2Int): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(flag2Int.value())
        return Flag2Int.flag2int(operand, flag2Int.type())
    }

    override fun visit(zext: ZeroExtend): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(zext.value())
        return ZeroExtend.zext(operand, zext.type())
    }

    override fun visit(itofp: Int2Float): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(itofp.value())
        return Int2Float.int2fp(operand, itofp.type())
    }

    override fun visit(utofp: Unsigned2Float): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(utofp.value())
        return Unsigned2Float.uint2fp(operand, utofp.type())
    }

    override fun visit(sext: SignExtend): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(sext.value())
        return SignExtend.sext(operand, sext.type())
    }

    override fun visit(trunc: Truncate): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(trunc.value())
        return Truncate.trunc(operand, trunc.type())
    }

    override fun visit(fptruncate: FpTruncate): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(fptruncate.value())
        return FpTruncate.fptrunc(operand, fptruncate.type())
    }

    override fun visit(fpext: FpExtend): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(fpext.value())
        return FpExtend.fpext(operand, fpext.type())
    }

    override fun visit(fptosi: Float2Int): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(fptosi.value())
        return Float2Int.fp2int(operand, fptosi.type())
    }

    override fun visit(copy: Copy): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(copy.origin())
        return Copy.copy(operand)
    }

    override fun visit(move: Move): InstBuilder<Instruction> {
        val fromValue = mapUsage<Value>(move.source())
        val toValue   = mapUsage<Generate>(move.destination())
        return Move.move(toValue, fromValue)
    }

    override fun visit(downStackFrame: DownStackFrame): InstBuilder<Instruction> {
        return DownStackFrame.dsf(downStackFrame.call())
    }

    override fun visit(gep: GetElementPtr): InstBuilder<Instruction> {
        val source = mapUsage<Value>(gep.source())
        val index  = mapUsage<Value>(gep.index())

        return GetElementPtr.gep(source, gep.basicType, index)
    }

    override fun visit(gfp: GetFieldPtr): InstBuilder<Instruction> {
        val source = mapUsage<Value>(gfp.source())
        return GetFieldPtr.gfp(source, gfp.basicType, gfp.index())
    }

    override fun visit(icmp: IntCompare): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(icmp.first())
        val second = mapUsage<Value>(icmp.second())

        return IntCompare.icmp(first, icmp.predicate(), second)
    }

    override fun visit(fcmp: FloatCompare): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(fcmp.first())
        val second = mapUsage<Value>(fcmp.second())

        return FloatCompare.fcmp(first, fcmp.predicate(), second)
    }

    override fun visit(load: Load): InstBuilder<Instruction> {
        val pointer = mapUsage<Value>(load.operand())
        return Load.load(load.type(), pointer)
    }

    override fun visit(phi: Phi): InstBuilder<Instruction> {
        val newUsages = arrayFrom(phi.operands()) { it -> it }
        val newIncoming = phi.incoming().mapTo(arrayListOf()) { mapBlock(it) } //TODO
        return Phi.phi(newIncoming, newUsages) //TODO
    }

    override fun visit(phi: UncompletedPhi): InstBuilder<Instruction> {
        throw RuntimeException("Unsupported operation")
    }

    override fun visit(returnValue: ReturnValue): InstBuilder<Instruction> {
        val value = arrayFrom(mapOperands(returnValue))
        return ReturnValue.ret(returnValue.type(), value)
    }

    override fun visit(returnVoid: ReturnVoid): InstBuilder<Instruction> {
        return ReturnVoid.ret()
    }

    override fun visit(indirectionCall: IndirectionCall): InstBuilder<Instruction> {
        val newUsages = mapArguments(indirectionCall)
        val pointer   = mapUsage<Value>(indirectionCall.pointer())
        val target    = mapBlock(indirectionCall.target())

        return IndirectionCall.call(pointer, indirectionCall.prototype(), newUsages, cloneAttributes(indirectionCall), target)
    }

    override fun visit(indirectionVoidCall: IndirectionVoidCall): InstBuilder<Instruction> {
        val newUsages = mapArguments(indirectionVoidCall)
        val pointer   = mapUsage<Value>(indirectionVoidCall.pointer())
        val target    = mapBlock(indirectionVoidCall.target())

        return IndirectionVoidCall.call(pointer, indirectionVoidCall.prototype(), newUsages, cloneAttributes(indirectionVoidCall), target)
    }

    override fun visit(select: Select): InstBuilder<Instruction> {
        val condition = mapUsage<Value>(select.condition())
        val onTrue    = mapUsage<Value>(select.onTrue())
        val onFalse   = mapUsage<Value>(select.onFalse())

        return Select.select(condition, select.type(), onTrue, onFalse)
    }

    override fun visit(store: Store): InstBuilder<Instruction> {
        val pointer = mapUsage<Value>(store.pointer())
        val value   = mapUsage<Value>(store.value())

        return Store.store(pointer, value)
    }

    override fun visit(upStackFrame: UpStackFrame): InstBuilder<Instruction> {
        return UpStackFrame.usf(upStackFrame.call())
    }

    override fun visit(voidCall: VoidCall): InstBuilder<Instruction> {
        val newUsages = mapArguments(voidCall)
        val target    = mapBlock(voidCall.target())
        return VoidCall.call(voidCall.prototype(), newUsages, cloneAttributes(voidCall), target)
    }

    override fun visit(int2ptr: Int2Pointer): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(int2ptr.value())
        return Int2Pointer.int2ptr(operand)
    }

    override fun visit(ptr2Int: Pointer2Int): InstBuilder<Instruction> {
        val operand = mapUsage<Value>(ptr2Int.value())
        return Pointer2Int.ptr2int(operand, ptr2Int.type())
    }

    override fun visit(indexedLoad: IndexedLoad): InstBuilder<Instruction> {
        val fromValue = mapUsage<Value>(indexedLoad.origin())
        val toValue   = mapUsage<Value>(indexedLoad.index())

        return IndexedLoad.load(fromValue, indexedLoad.type(), toValue)
    }

    override fun visit(store: StoreOnStack): InstBuilder<Instruction> {
        val source = mapUsage<Value>(store.source())
        val index  = mapUsage<Value>(store.index())
        val dest   = mapUsage<Value>(store.destination())

        return StoreOnStack.store(dest, index, source)
    }

    override fun visit(loadst: LoadFromStack): InstBuilder<Instruction> {
        val origin = mapUsage<Value>(loadst.origin())
        val index  = mapUsage<Value>(loadst.index())

        return LoadFromStack.load(origin, loadst.type(), index)
    }

    override fun visit(leaStack: LeaStack): InstBuilder<Instruction> {
        val origin = mapUsage<Value>(leaStack.origin())
        val index  = mapUsage<Value>(leaStack.index())

        return LeaStack.lea(origin, leaStack.type(), index)
    }

    override fun visit(tupleDiv: TupleDiv): InstBuilder<Instruction> {
        val first  = mapUsage<Value>(tupleDiv.first())
        val second = mapUsage<Value>(tupleDiv.second())

        return TupleDiv.div(first,  second)
    }

    override fun visit(proj: Projection): InstBuilder<Instruction> {
        val origin = mapUsage<Value>(proj.tuple())
        return Projection.proj(origin, proj.index())
    }

    override fun visit(switch: Switch): InstBuilder<Instruction> {
        val newValue   = mapUsage<Value>(switch.value())
        val newDefault = mapBlock(switch.default())
        val newTargets = arrayFrom(switch.targets()) { mapBlock(it) }
        val newTable   = arrayFrom(switch.table()) { it }

        return Switch.switch(newValue, newDefault, newTable, newTargets)
    }

    override fun visit(tupleCall: IndirectionTupleCall): InstBuilder<Instruction> {
        val newUsages = mapArguments(tupleCall)
        val target    = mapBlock(tupleCall.target())
        val pointer   = mapUsage<Value>(tupleCall.pointer())

        return IndirectionTupleCall.call(pointer, tupleCall.prototype(), newUsages, cloneAttributes(tupleCall), target)
    }

    override fun visit(intrinsic: Intrinsic): InstBuilder<Instruction> {
        val newUsages = arrayFrom(intrinsic.inputs()) {it -> mapUsage<Value>(it) }

        val target = mapBlock(intrinsic.target())
        return Intrinsic.intrinsic(newUsages, intrinsic.implementor, target)
    }

    override fun visit(memcpy: Memcpy): InstBuilder<Instruction> {
        val dst = mapUsage<Value>(memcpy.destination())
        val src = mapUsage<Value>(memcpy.source())

        return Memcpy.memcpy(dst, src, memcpy.length())
    }

    override fun visit(move: MoveByIndex): InstBuilder<Instruction> {
        val index   = mapUsage<Value>(move.index())
        val toValue = mapUsage<Value>(move.destination())
        val source  = mapUsage<Value>(move.source())

        return MoveByIndex.move(toValue, index, source)
    }

    companion object {
        fun copy(old: FunctionData): FunctionData {
            return CopyCFG(old).copy()
        }
    }
}