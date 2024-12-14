package ir.module.block

import ir.types.*
import ir.value.*
import common.arrayFrom
import common.assertion
import ir.instruction.*
import ir.instruction.lir.*
import common.LeakedLinkedList
import ir.attributes.FunctionAttribute
import ir.intrinsic.IntrinsicProvider
import ir.module.DirectFunctionPrototype
import ir.module.IndirectFunctionPrototype
import ir.module.ModificationCounter
import ir.value.constant.IntegerConstant
import ir.value.constant.UndefValue
import ir.value.constant.UnsignedIntegerConstant


class Block private constructor(private val mc: ModificationCounter, override val index: Int):
    AnyInstructionFabric, AnyBlock, Iterable<Instruction> {
    private val instructions = InstructionList()
    private val predecessors = arrayListOf<Block>()
    private val successors   = arrayListOf<Block>()

    private var instructionIndex: Int = 0

    override fun predecessors(): List<Block> {
        return predecessors
    }

    override fun successors(): List<Block> {
        return successors
    }

    override fun last(): TerminateInstruction {
        return lastOrNull() ?:
            throw IllegalStateException("Last instruction is not terminate: bb=$this, last=${instructions.lastOrNull()}")
    }

    fun lastOrNull(): TerminateInstruction? {
        if (instructions.isEmpty()) {
            return null
        }

        val last = instructions.last()
        if (last !is TerminateInstruction) {
            return null
        }

        return last
    }

    override fun begin(): Instruction {
        assertion(instructions.isNotEmpty()) {
            "bb=$this must have any instructions"
        }

        return instructions.first()
    }

    val size
        get(): Int = instructions.size

    private fun updateSuccessor(old: Block, new: Block): Int {
        val index = successors.indexOf(old)
        if (index == -1) {
            throw RuntimeException("Out of index: old=$old")
        }

        new.predecessors.add(this)
        successors[index] = new
        return index
    }

    private fun removePredecessors(old: Block) {
        assertion(predecessors.contains(old)) {
            "old=$old is not in bb=$this"
        }

        predecessors.remove(old)
    }

    fun instructions(fn: (Instruction) -> Unit) {
        instructions.forEach(fn)
    }

    fun transform(fn: (Instruction) -> Instruction?) {
        instructions.transform { fn(it) }
    }

    fun<V: Value> updateUsages(localValue: UsableValue, replacement: () -> V): V = mc.df {
        return@df UsableValue.updateUsages(localValue, replacement)
    }

    fun updateDF(instruction: Instruction, closure: (Value) -> Value) = mc.df {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        instruction.update { closure(it) }
    }

    fun updateDF(instruction: Instruction, index: Int, value: Value) = mc.df {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        instruction.update(index, value)
    }

    fun updateDF(phi: Phi, closure: (Block, Value) -> Value) = mc.df { //TODO remove this after UncompletedPhi instruction creation
        assertion(phi.owner() === this) {
            "phi=$phi is not in bb=$this"
        }

        phi.zipWithIndex { bb, value, idx ->
            phi.update(idx, closure(bb, value))
        }
    }

    fun updateCF(phi: Phi, closure: (Block, Value) -> Block) = mc.cf {
        assertion(phi.owner() === this) {
            "phi=$phi is not in bb=$this"
        }

        phi.zipWithIndex { bb, value, idx ->
            phi.updateIncoming(closure(bb, value), idx)
        }
    }

    private fun updatePhi(oldSucc: Block, newSucc: Block) {
        oldSucc.phis { phi ->
            oldSucc.updateCF(phi) { oldBB, _ ->
                if (oldBB == this) newSucc else oldBB
            }
        }
    }

    fun updateCF(currentSuccessor: Block, newSuccessor: Block) = mc.cf {
        val index = updateSuccessor(currentSuccessor, newSuccessor)
        currentSuccessor.removePredecessors(this)

        val terminateInstruction = last()
        terminateInstruction.updateTarget(newSuccessor, index)

        updatePhi(currentSuccessor, newSuccessor)
    }

    override fun contains(instruction: Instruction): Boolean {
        return instructions.contains(instruction)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is BlockViewer) {
            return index == other.index
        }

        if (other == null || this::class != other::class) return false
        other as Block

        return index == other.index
    }

    override fun hashCode(): Int {
        return index
    }

    fun isEmpty(): Boolean {
        return instructions.isEmpty()
    }

    override operator fun iterator(): Iterator<Instruction> {
        return instructions.iterator()
    }

    fun phis(fn: (Phi) -> Unit) = instructions.forEach {
        if (it !is Phi) {
            return@forEach // Assume that phi functions are in the beginning of bb.
        }

        fn(it)
    }

    fun kill(instruction: Instruction, replacement: Value): Instruction? = mc.df {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        val next = instruction.prev()
        if (instruction is UsableValue) {
            updateUsages(instruction) { replacement }
        }

        val removed = remove(instruction)
        removed.destroy()
        return@df next
    }

    private fun remove(instruction: Instruction): Instruction {
        return instructions.remove(instruction)
    }

    override fun not(value: Value): Not = mc.df {
        return@df put(Not.not(value))
    }

    override fun neg(value: Value): Neg = mc.df {
        return@df put(Neg.neg(value))
    }

    override fun add(a: Value, b: Value): Add {
        return put(Add.add(a, b))
    }

    override fun and(a: Value, b: Value): And {
        return put(And.and(a, b))
    }

    override fun or(a: Value, b: Value): Or {
        return put(Or.or(a, b))
    }

    override fun shl(a: Value, b: Value): Shl {
        return put(Shl.shl(a, b))
    }

    override fun shr(a: Value, b: Value): Shr {
        return put(Shr.shr(a, b))
    }

    override fun div(a: Value, b: Value): Div {
        return put(Div.div(a, b))
    }

    override fun sub(a: Value, b: Value): Sub {
        return put(Sub.sub(a, b))
    }

    override fun xor(a: Value, b: Value): Xor {
        return put(Xor.xor(a, b))
    }

    override fun mul(a: Value, b: Value): Mul {
        return put(Mul.mul(a, b))
    }

    override fun tupleDiv(a: Value, b: Value): TupleDiv = mc.df {
        return@df put(TupleDiv.div(a, b))
    }

    override fun icmp(a: Value, predicate: IntPredicate, b: Value): IntCompare = mc.df {
        return@df put(IntCompare.icmp(a, predicate, b))
    }

    override fun fcmp(a: Value, predicate: FloatPredicate, b: Value): FloatCompare = mc.df {
        return@df put(FloatCompare.fcmp(a, predicate, b))
    }

    override fun load(loadedType: PrimitiveType, ptr: Value): Load = mc.df {
        return@df put(Load.load(loadedType, ptr))
    }

    override fun store(ptr: Value, value: Value): Store = mc.df {
        return@df put(Store.store(ptr, value))
    }

    override fun call(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): Call = mc.dfANDcf {
        return@dfANDcf put(Call.call(func, args, attributes, target as Block))
    }

    override fun tupleCall(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): TupleCall = mc.dfANDcf {
        return@dfANDcf put(TupleCall.call(func, args, attributes, target as Block))
    }

    override fun vcall(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): VoidCall = mc.dfANDcf {
        return@dfANDcf put(VoidCall.call(func, args, attributes, target as Block))
    }

    override fun icall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): IndirectionCall = mc.dfANDcf {
        require(func.returnType() != VoidType)
        return@dfANDcf put(IndirectionCall.call(pointer, func, args, attributes, target as Block))
    }

    override fun ivcall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Label): IndirectionVoidCall = mc.dfANDcf {
        return@dfANDcf put(IndirectionVoidCall.call(pointer, func, args, attributes, target as Block))
    }

    override fun branch(target: Block): Branch = mc.cf {
        return@cf put(Branch.br(target))
    }

    override fun branchCond(value: Value, onTrue: Label, onFalse: Label): BranchCond = mc.dfANDcf {
        return@dfANDcf put(BranchCond.br(value, onTrue as Block, onFalse as Block))
    }

    override fun alloc(ty: NonTrivialType): Alloc = mc.df {
        return@df put(Alloc.alloc(ty))
    }

    override fun ret(returnType: Type, values: Array<Value>): Return = mc.dfANDcf {
        return@dfANDcf put(ReturnValue.ret(returnType, values))
    }

    override fun retVoid(): ReturnVoid = mc.cf {
        return@cf put(ReturnVoid.ret())
    }

    override fun gep(source: Value, elementType: NonTrivialType, index: Value): GetElementPtr = mc.df {
        return@df put(GetElementPtr.gep(elementType, source, index))
    }

    override fun gfp(source: Value, ty: AggregateType, index: IntegerConstant): GetFieldPtr = mc.df {
        return@df put(GetFieldPtr.gfp(ty, source, index))
    }

    override fun flag2int(value: Value, ty: IntegerType): Flag2Int = mc.df {
        return@df put(Flag2Int.flag2int(ty, value))
    }

    override fun int2fp(value: Value, ty: FloatingPointType): Int2Float = mc.df {
        return@df put(Int2Float.int2fp(ty, value))
    }

    override fun uint2fp(value: Value, ty: FloatingPointType): Unsigned2Float {
        return put(Unsigned2Float.uint2fp(ty, value))
    }

    override fun bitcast(value: Value, ty: IntegerType): Bitcast = mc.df {
        return@df put(Bitcast.bitcast(ty, value))
    }

    override fun zext(value: Value, toType: UnsignedIntType): ZeroExtend = mc.df {
        return@df put(ZeroExtend.zext(value, toType))
    }

    override fun sext(value: Value, toType: SignedIntType): SignExtend = mc.df {
        return@df put(SignExtend.sext(value, toType))
    }

    override fun trunc(value: Value, toType: IntegerType): Truncate = mc.df {
        return@df put(Truncate.trunc(value, toType))
    }

    override fun fptrunc(value: Value, toType: FloatingPointType): FpTruncate = mc.df {
        return@df put(FpTruncate.fptrunc(toType, value))
    }

    override fun fpext(value: Value, toType: FloatingPointType): FpExtend = mc.df {
        return@df put(FpExtend.fpext(toType, value))
    }

    override fun fp2Int(value: Value, toType: IntegerType): Float2Int = mc.df {
        return@df put(Float2Int.fp2int(toType, value))
    }

    override fun select(cond: Value, type: IntegerType, onTrue: Value, onFalse: Value): Select = mc.df {
        return@df put(Select.select(cond, type, onTrue, onFalse))
    }

    override fun phi(incoming: List<Value>, labels: List<Label>): Phi = mc.df {
        val bbs = labels.mapTo(arrayListOf()) { it as Block }
        return@df put(Phi.phi(bbs, incoming.toTypedArray()))
    }

    override fun int2ptr(value: Value): Int2Pointer = mc.df {
        return@df put(Int2Pointer.int2ptr(value))
    }

    override fun ptr2int(value: Value, toType: IntegerType): Pointer2Int = mc.df {
        return@df put(Pointer2Int.ptr2int(toType, value))
    }

    override fun memcpy(dst: Value, src: Value, length: UnsignedIntegerConstant): Memcpy = mc.df {
        return@df put(Memcpy.memcpy(dst, src, length))
    }

    override fun proj(tuple: Value, index: Int): Projection = mc.df {
        return@df put(Projection.proj(tuple, index))
    }

    override fun switch(value: Value, default: Label, table: List<IntegerConstant>, targets: List<Label>): Switch = mc.dfANDcf {
        val resolved = arrayFrom(targets) { bb -> bb as Block }
        return@dfANDcf put(Switch.switch(value, default as Block, table.toTypedArray(), resolved))
    }

    override fun intrinsic(inputs: List<Value>, implementor: IntrinsicProvider, target: Label): Intrinsic {
        return put(Intrinsic.intrinsic(inputs.toTypedArray(), implementor, target as Block))
    }

    override fun downStackFrame(callable: Callable): DownStackFrame = mc.df {
        return@df put(DownStackFrame.dsf(callable))
    }

    override fun upStackFrame(callable: Callable): UpStackFrame = mc.df {
        return@df put(UpStackFrame.usf(callable))
    }

    override fun uncompletedPhi(ty: PrimitiveType, incoming: Value): Phi = mc.df {
        val blocks = predecessors().mapTo(arrayListOf()) { it }
        return@df put(Phi.phiUncompleted(ty, incoming, blocks))
    }

    override fun gen(ty: NonTrivialType): Generate = mc.df {
        return@df put(Generate.gen(ty))
    }

    override fun lea(source: Value): Lea = mc.df {
        return@df put(Lea.lea(source))
    }

    fun uncompletedPhi(incoming: List<Value>, labels: List<Block>): Phi = mc.df {
        val blocks = labels.mapTo(arrayListOf()) { it }
        return@df put(Phi.phi(blocks, incoming.toTypedArray()))
    }

    override fun copy(value: Value): Copy = mc.df {
        return@df put(Copy.copy(value))
    }

    override fun move(dst: UsableValue, fromValue: Value): Move = mc.df {
        return@df put(Move.move(dst, fromValue))
    }

    override fun move(dst: Value, index: Value, src: Value): MoveByIndex = mc.df {
        return@df put(MoveByIndex.move(dst, index, src))
    }

    override fun indexedLoad(origin: Value, loadedType: PrimitiveType, index: Value): IndexedLoad = mc.df {
        return@df put(IndexedLoad.load(loadedType, origin, index))
    }

    override fun storeOnStack(destination: Value, index: Value, source: Value): StoreOnStack = mc.df {
        return@df put(StoreOnStack.store(destination, index, source))
    }

    override fun loadFromStack(origin: Value, loadedType: PrimitiveType, index: Value): LoadFromStack = mc.df {
        return@df put(LoadFromStack.load(loadedType, origin, index))
    }

    override fun leaStack(origin: Value, loadedType: PrimitiveType, index: Value): LeaStack = mc.df {
        return@df put(LeaStack.lea(origin, loadedType, index))
    }

    fun idom(instruction: Instruction): Instruction? {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        return instruction.prev()
    }

    fun ipdom(instruction: Instruction): Instruction? {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        return instruction.next()
    }

    private fun makeEdge(to: Block) = mc.cf {
        successors.add(to)
        to.predecessors.add(this)
    }

    internal fun removeEdge(to: Block) = mc.cf {
        successors.remove(to)
        to.predecessors.remove(this)
    }

    private fun allocateValue(): Int {
        val currentValue = instructionIndex
        instructionIndex += 1
        return currentValue
    }

    fun<T: Instruction> put(f: InstBuilder<T>): T {
        val value = allocateValue()
        val instruction = f(value, this)
        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

        instructions.addAfter(null, instruction)
        return instruction
    }

    fun<T: Instruction> putAfter(after: Instruction, f: InstBuilder<T>): T {
        assertion(after.owner() === this) {
            "after=$after is not in bb=$this"
        }

        val instruction = f(allocateValue(), this)
        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

        instructions.addAfter(after, instruction)
        return instruction
    }

    fun<T: Instruction> putBefore(before: Instruction, f: InstBuilder<T>): T {
        assertion(before.owner() === this) {
            "before=$before is not in bb=$this"
        }

        val instruction = f(allocateValue(), this)
        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

        instructions.addBefore(before, instruction)
        return instruction
    }

    fun<T: Instruction> prepend(f: InstBuilder<T>): T {
        val instruction = f(allocateValue(), this)
        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

        instructions.addBefore(null, instruction)
        return instruction
    }

    inline fun<reified T: Instruction> replace(instruction: Instruction, noinline builder: InstBuilder<T>): T {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        val newInstruction = putBefore(instruction, builder)
        if (instruction is UsableValue) {
            assertion(newInstruction is UsableValue) {
                "should be local value, but newInstruction=$newInstruction"
            }
            updateUsages(instruction) { newInstruction as UsableValue }
        }
        kill(instruction, UndefValue)
        return newInstruction
    }

    override fun toString(): String {
        return if (index == 0) {
            "entry"
        } else {
            "L$index"
        }
    }

    companion object {
        fun empty(mc: ModificationCounter, blockIndex: Int): Block {
            return Block(mc, blockIndex)
        }
    }
}

private class InstructionList: LeakedLinkedList<Instruction>()