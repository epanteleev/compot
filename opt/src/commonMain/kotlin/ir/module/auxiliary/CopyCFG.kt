package ir.module.auxiliary

import common.arrayFrom
import common.forEachWith
import common.intMapOf
import ir.global.GlobalSymbol
import ir.instruction.*
import ir.instruction.Copy
import ir.instruction.lir.*
import ir.instruction.utils.IRInstructionVisitor
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label
import ir.types.NonTrivialType
import ir.value.*


class CopyCFG private constructor(private val fd: FunctionData) : IRInstructionVisitor<LocalValue?>() {
    private val oldBasicBlocks: BasicBlocks = fd.blocks
    private val oldValuesToNew = hashMapOf<LocalValue, LocalValue>()
    private val newCFG = BasicBlocks.create()
    private val oldToNewBlock = setupNewBasicBlock()
    private var currentBB: Block? = null

    private val bb: Block
        get() = currentBB?: throw RuntimeException("currentBB is null")

    private fun setupNewBasicBlock(): Map<Block, Block> {
        val oldToNew = intMapOf<Block, Block>(oldBasicBlocks.size()) { it.index }

        for (old in oldBasicBlocks.blocks()) {
            if (old.index == 0) { //TODO
                oldToNew[old] = newCFG.begin()
                continue
            }
            oldToNew[old] = newCFG.createBlock()
        }

        return oldToNew
    }

    private fun copyArguments(): List<ArgumentValue> {
        val newArgs = arrayListOf<ArgumentValue>()
        fd.arguments().forEachWith(fd.prototype.arguments()) { arg, type, i ->
            if (type !is NonTrivialType) {
                throw IllegalStateException("unexpected type for argument=$arg")
            }

            val newArg = ArgumentValue(i, type)
            oldValuesToNew[arg] = newArg
            newArgs.add(newArg)
        }
        return newArgs
    }

    fun copy(): FunctionData {
        val newArgs = copyArguments()
        copyBasicBlocks()
        return FunctionData.create(fd.prototype, newCFG, newArgs)
    }

    private fun copyBasicBlocks() {
        for (bb in oldBasicBlocks.preorder()) {
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
        val newInstruction = inst.visit(this)
        if (inst is LocalValue) {
            oldValuesToNew[inst] = newInstruction as LocalValue
        }

        return newInstruction
    }

    private fun updatePhis() {
        for (bb in newCFG) {
            bb.phis { phi ->
                phi.updateDataFlow { _, value -> mapUsage(value) }
            }
        }
    }

    private inline fun<reified T> mapUsage(old: Value): T {
        if (old is Constant || old is GlobalSymbol) {
            return old as T
        }
        val value = oldValuesToNew[old]?: throw RuntimeException("cannot find localValue=${old}")
        if (value !is T) {
            throw RuntimeException("unexpected type for value=$value")
        }

        return value
    }

    private fun mapBlock(old: Block): Block {
        return oldToNewBlock[old]?: throw RuntimeException("cannot find new block, oldBlock=$old")
    }

    override fun visit(alloc: Alloc): ValueInstruction {
        return bb.alloc(alloc.allocatedType)
    }

    override fun visit(generate: Generate): ValueInstruction {
        return bb.gen(generate.type())
    }

    override fun visit(lea: Lea): ValueInstruction {
        val operand = mapUsage<Value>(lea.operand())
        return bb.lea(operand)
    }

    override fun visit(binary: ArithmeticBinary): ValueInstruction {
        val first  = mapUsage<Value>(binary.first())
        val second = mapUsage<Value>(binary.second())

        return bb.arithmeticBinary(first, binary.op, second)
    }

    override fun visit(neg: Neg): ValueInstruction {
        val operand = mapUsage<Value>(neg.operand())
        return bb.neg(operand)
    }

    override fun visit(not: Not): ValueInstruction {
        val operand = mapUsage<Value>(not.operand())
        return bb.not(operand)
    }

    override fun visit(branch: Branch): ValueInstruction? {
        bb.branch(mapBlock(branch.target()))
        return null
    }

    override fun visit(branchCond: BranchCond): ValueInstruction? {
        val condition = mapUsage<Value>(branchCond.condition())
        val onTrue    = mapBlock(branchCond.onTrue())
        val onFalse   = mapBlock(branchCond.onFalse())
        bb.branchCond(condition, onTrue, onFalse)

        return null
    }

    override fun visit(call: Call): LocalValue {
        val newUsages = call.operands().map { mapUsage<Value>(it) }
        val target    = mapBlock(call.target())

        return bb.call(call.prototype(), newUsages, target)
    }

    override fun visit(tupleCall: TupleCall): LocalValue? {
        val newUsages = tupleCall.operands().map { mapUsage<Value>(it) }
        val target    = mapBlock(tupleCall.target())

        bb.tupleCall(tupleCall.prototype(), newUsages, target)
        return null
    }

    override fun visit(bitcast: Bitcast): ValueInstruction {
        val operand = mapUsage<Value>(bitcast.value())
        return bb.bitcast(operand, bitcast.type())
    }

    override fun visit(flag2Int: Flag2Int): ValueInstruction {
        val operand = mapUsage<Value>(flag2Int.value())
        return bb.flag2int(operand, flag2Int.type())
    }

    override fun visit(zext: ZeroExtend): ValueInstruction {
        val operand = mapUsage<Value>(zext.value())
        return bb.zext(operand, zext.type())
    }

    override fun visit(itofp: Int2Float): ValueInstruction {
        val operand = mapUsage<Value>(itofp.value())
        return bb.int2fp(operand, itofp.type())
    }

    override fun visit(sext: SignExtend): ValueInstruction {
        val operand = mapUsage<Value>(sext.value())
        return bb.sext(operand, sext.type())
    }

    override fun visit(trunc: Truncate): ValueInstruction {
        val operand = mapUsage<Value>(trunc.value())
        return bb.trunc(operand, trunc.type())
    }

    override fun visit(fptruncate: FpTruncate): ValueInstruction {
        val operand = mapUsage<Value>(fptruncate.value())
        return bb.fptrunc(operand, fptruncate.type())
    }

    override fun visit(fpext: FpExtend): ValueInstruction {
        val operand = mapUsage<Value>(fpext.value())
        return bb.fpext(operand, fpext.type())
    }

    override fun visit(fptosi: FloatToInt): ValueInstruction {
        val operand = mapUsage<Value>(fptosi.value())
        return bb.fp2Int(operand, fptosi.type())
    }

    override fun visit(copy: Copy): ValueInstruction {
        val operand = mapUsage<Value>(copy.origin())
        return bb.copy(operand)
    }

    override fun visit(move: Move): ValueInstruction? {
        val fromValue = mapUsage<Value>(move.source())
        val toValue   = mapUsage<Generate>(move.destination())
        bb.move(toValue, fromValue)
        return null
    }

    override fun visit(downStackFrame: DownStackFrame): ValueInstruction? {
        bb.downStackFrame(downStackFrame.call())
        return null
    }

    override fun visit(gep: GetElementPtr): ValueInstruction {
        val source = mapUsage<Value>(gep.source())
        val index  = mapUsage<Value>(gep.index())

        return bb.gep(source, gep.basicType, index)
    }

    override fun visit(gfp: GetFieldPtr): ValueInstruction {
        val source = mapUsage<Value>(gfp.source())
        return bb.gfp(source, gfp.basicType, gfp.indexes().copyOf())
    }

    override fun visit(icmp: SignedIntCompare): ValueInstruction {
        val first  = mapUsage<Value>(icmp.first())
        val second = mapUsage<Value>(icmp.second())

        return bb.icmp(first, icmp.predicate(), second)
    }

    override fun visit(fcmp: FloatCompare): ValueInstruction {
        val first  = mapUsage<Value>(fcmp.first())
        val second = mapUsage<Value>(fcmp.second())

        return bb.fcmp(first, fcmp.predicate(), second)
    }

    override fun visit(load: Load): ValueInstruction {
        val pointer = mapUsage<Value>(load.operand())
        return bb.load(load.type(), pointer)
    }

    override fun visit(phi: Phi): ValueInstruction {
        val newUsages   = phi.operands().toList()
        val newIncoming = phi.incoming().mapTo(arrayListOf()) { mapBlock(it) } //TODO

        return bb.uncompletedPhi(phi.type(), newUsages, newIncoming) //TODO
    }

    override fun visit(returnValue: ReturnValue): ValueInstruction? {
        val value = arrayFrom(returnValue.operands()) { mapUsage<Value>(it) }
        bb.ret(returnValue.type(), value)
        return null
    }

    override fun visit(returnVoid: ReturnVoid): ValueInstruction? {
        bb.retVoid()
        return null
    }

    override fun visit(indirectionCall: IndirectionCall): LocalValue {
        val newUsages = indirectionCall.operands().map { mapUsage<Value>(it) }
        val pointer   = mapUsage<Value>(indirectionCall.pointer())

        val target    = mapBlock(indirectionCall.target())

        return bb.icall(pointer, indirectionCall.prototype(), newUsages, target)
    }

    override fun visit(indirectionVoidCall: IndirectionVoidCall): ValueInstruction? {
        val newUsages = indirectionVoidCall.operands().map { mapUsage<Value>(it) }
        val pointer   = mapUsage<Value>(indirectionVoidCall.pointer())
        val target    = mapBlock(indirectionVoidCall.target())

        bb.ivcall(pointer, indirectionVoidCall.prototype(), newUsages, target)
        return null
    }

    override fun visit(select: Select): ValueInstruction {
        val condition = mapUsage<Value>(select.condition())
        val onTrue    = mapUsage<Value>(select.onTrue())
        val onFalse   = mapUsage<Value>(select.onFalse())

        return bb.select(condition, select.type(), onTrue, onFalse)
    }

    override fun visit(store: Store): ValueInstruction? {
        val pointer = mapUsage<Value>(store.pointer())
        val value   = mapUsage<Value>(store.value())

        bb.store(pointer, value)
        return null
    }

    override fun visit(upStackFrame: UpStackFrame): ValueInstruction? {
        bb.upStackFrame(upStackFrame.call())
        return null
    }

    override fun visit(voidCall: VoidCall): ValueInstruction? {
        val newUsages = voidCall.operands().map { mapUsage<Value>(it) }
        val target    = mapBlock(voidCall.target())
        bb.vcall(voidCall.prototype(), newUsages, target)
        return null
    }

    override fun visit(int2ptr: Int2Pointer): ValueInstruction {
        val operand = mapUsage<Value>(int2ptr.value())
        return bb.int2ptr(operand)
    }

    override fun visit(ptr2Int: Pointer2Int): ValueInstruction {
        val operand = mapUsage<Value>(ptr2Int.value())
        return bb.ptr2int(operand, ptr2Int.type())
    }

    override fun visit(copy: IndexedLoad): ValueInstruction {
        val fromValue = mapUsage<Value>(copy.origin())
        val toValue   = mapUsage<Value>(copy.index())

        return bb.indexedLoad(fromValue, copy.type(), toValue)
    }

    override fun visit(store: StoreOnStack): ValueInstruction? {
        val source = mapUsage<Value>(store.source())
        val index  = mapUsage<Value>(store.index())
        val dest   = mapUsage<Value>(store.destination())

        bb.storeOnStack(dest, index, source)
        return null
    }

    override fun visit(loadst: LoadFromStack): ValueInstruction {
        val origin = mapUsage<Value>(loadst.origin())
        val index  = mapUsage<Value>(loadst.index())

        return bb.loadFromStack(origin, loadst.type(), index)
    }

    override fun visit(leaStack: LeaStack): ValueInstruction {
        val origin = mapUsage<Value>(leaStack.origin())
        val index  = mapUsage<Value>(leaStack.index())

        return bb.leaStack(origin, leaStack.type(), index)
    }

    override fun visit(binary: TupleDiv): LocalValue {
        val first  = mapUsage<Value>(binary.first())
        val second = mapUsage<Value>(binary.second())

        return bb.tupleDiv(first,  second)
    }

    override fun visit(proj: Projection): LocalValue {
        val origin = mapUsage<Value>(proj.tuple())
        return bb.proj(origin, proj.index())
    }

    override fun visit(switch: Switch): LocalValue? {
        val newValue   = mapUsage<Value>(switch.value())
        val newDefault = mapBlock(switch.default())
        val newTargets = switch.targets().mapTo(arrayListOf()) { mapBlock(it) }
        val newTable   = switch.table().mapTo(arrayListOf()) { it }

        bb.switch(newValue, newDefault, newTable, newTargets)
        return null
    }

    override fun visit(memcpy: Memcpy): ValueInstruction? {
        val dst = mapUsage<Value>(memcpy.destination())
        val src = mapUsage<Value>(memcpy.source())

        bb.memcpy(dst, src, memcpy.length())
        return null
    }

    override fun visit(move: MoveByIndex): ValueInstruction? {
        val index   = mapUsage<Value>(move.index())
        val toValue = mapUsage<Value>(move.destination())
        val source  = mapUsage<Value>(move.source())

        bb.move(toValue, index, source)
        return null
    }

    companion object {
        fun copy(old: FunctionData): FunctionData {
            return CopyCFG(old).copy()
        }
    }
}