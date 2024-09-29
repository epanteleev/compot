package ir.module.block

import ir.types.*
import ir.value.*
import common.arrayFrom
import common.assertion
import ir.instruction.*
import ir.instruction.lir.*
import common.LeakedLinkedList
import ir.module.AnyFunctionPrototype
import ir.module.DirectFunctionPrototype
import ir.module.IndirectFunctionPrototype
import ir.module.ModificationCounter


class Block private constructor(private val mc: ModificationCounter, override val index: Int):
    AnyInstructionFabric, AnyBlock, Iterable<Instruction> {
    private val instructions = InstructionList()
    private val predecessors = arrayListOf<Block>()
    private val successors   = arrayListOf<Block>()

    private var insertionStrategy: InsertionStrategy = InsertAfter(null)
    private var instructionIndex: Int = 0

    abstract inner class InsertionStrategy {
        abstract fun insert(instruction: Instruction);
    }

    inner class InsertBefore(private val before: Instruction?) : InsertionStrategy() {
        override fun insert(instruction: Instruction) {
            instructions.addBefore(before, instruction)
        }
    }

    inner class InsertAfter(private val after: Instruction?) : InsertionStrategy() {
        override fun insert(instruction: Instruction) {
            if (after is TerminateInstruction) {
                throw IllegalStateException("Trying to insert instruction after terminate: bb=${after.owner()}, last='${instructions.lastOrNull()?.dump()}'")
            }
            instructions.addAfter(after, instruction)
        }
    }

    override fun predecessors(): List<Block> {
        return predecessors
    }

    override fun successors(): List<Block> {
        return successors
    }

    override fun last(): TerminateInstruction {
        return lastOrNull() ?:
            throw RuntimeException("Last instruction is not terminate: bb=$this, last=${instructions.lastOrNull()}")
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

    fun<T> prepend(builder: (AnyInstructionFabric) -> T): T {
        insertionStrategy = InsertBefore(null)
        return builder(this)
    }

    fun<T> insertAfter(after: Instruction, builder: (AnyInstructionFabric) -> T): T {
        assertion(after.owner() === this) {
            "after=$after is not in bb=$this"
        }

        insertionStrategy = InsertAfter(after)
        return builder(this)
    }

    fun<T> insertBefore(before: Instruction, builder: (AnyInstructionFabric) -> T): T {
        assertion(before.owner() === this) {
            "before=$before is not in bb=$this"
        }

        insertionStrategy = InsertBefore(before)
        return builder(this)
    }

    inline fun<reified T: Instruction> replace(instruction: Instruction, crossinline builder: (AnyInstructionFabric) -> T): T {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        val newInstruction = insertBefore(instruction) { builder(it) }
        if (instruction is LocalValue) {
            assertion(newInstruction is LocalValue) {
                "should be local value, but newInstruction=$newInstruction"
            }
            updateUsages(instruction) { newInstruction as LocalValue }
        }
        kill(instruction, Value.UNDEF)
        return newInstruction
    }

    fun<V: Value> updateUsages(localValue: LocalValue, replacement: () -> V): V = mc.df {
        val valueToReplace = replacement()
        for (user in localValue.release()) {
            for ((idxUse, use) in user.operands().withIndex()) {
                if (use !== localValue) {
                    continue
                }
                // New value can use the old value
                if (user == valueToReplace) {
                    continue
                }

                user.update(idxUse, valueToReplace)
            }
        }
        return@df valueToReplace
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
        if (instruction is LocalValue) {
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
        val valueType = value.type()
        require(valueType is IntegerType) {
            "should be integer type, but ty=$valueType"
        }

        return@df withOutput { Not.make(it, this, valueType, value) }
    }

    override fun neg(value: Value): Neg = mc.df {
        val valueType = value.type()
        require(valueType is ArithmeticType) {
            "should be integer type, but ty=$valueType"
        }

        return@df withOutput { Neg.make(it, this, valueType, value) }
    }

    override fun add(a: Value, b: Value): Add {
        return withOutput { Add.make(it, this, a, b) }
    }

    override fun and(a: Value, b: Value): And {
        return withOutput { And.make(it, this, a, b) }
    }

    override fun or(a: Value, b: Value): Or {
        return withOutput { Or.make(it, this, a, b) }
    }

    override fun shl(a: Value, b: Value): Shl {
        return withOutput { Shl.make(it, this, a, b) }
    }

    override fun shr(a: Value, b: Value): Shr {
        return withOutput { Shr.make(it, this, a, b) }
    }

    override fun div(a: Value, b: Value): Div {
        return withOutput { Div.make(it, this, a, b) }
    }

    override fun sub(a: Value, b: Value): Sub {
        return withOutput { Sub.make(it, this, a, b) }
    }

    override fun xor(a: Value, b: Value): Xor {
        return withOutput { Xor.make(it, this, a, b) }
    }

    override fun mul(a: Value, b: Value): Mul {
        return withOutput { Mul.make(it, this, a, b) }
    }

    override fun tupleDiv(a: Value, b: Value): TupleDiv = mc.df {
        val ty = a.type()
        require(ty is ArithmeticType) {
            "should be arithmetic type, but ty=$ty"
        }

        return@df withOutput { TupleDiv.make(it, this, ty, a, b) }
    }

    override fun icmp(a: Value, predicate: IntPredicate, b: Value): IntCompare = mc.df {
        return@df withOutput { IntCompare.make(it, this, a, predicate, b) }
    }

    override fun fcmp(a: Value, predicate: FloatPredicate, b: Value): FloatCompare = mc.df {
        return@df withOutput { FloatCompare.make(it, this, a, predicate, b) }
    }

    override fun load(loadedType: PrimitiveType, ptr: Value): Load = mc.df {
        return@df withOutput { Load.make(it, this, loadedType, ptr) }
    }

    override fun store(ptr: Value, value: Value): Store = mc.df {
        return@df withOutput { Store.make(it,this, ptr, value) }
    }

    override fun call(func: DirectFunctionPrototype, args: List<Value>, target: Label): Call = mc.dfANDcf {
        require(func.returnType() != Type.Void)
        return@dfANDcf addTerminate { Call.make(it, this, func, args, target as Block) }
    }

    override fun tupleCall(func: DirectFunctionPrototype, args: List<Value>, target: Label): TupleCall = mc.dfANDcf {
        require(func.returnType() is TupleType) {
            "should be tuple type, but ty=${func.returnType()}"
        }
        return@dfANDcf addTerminate { TupleCall.make(it, this, func, args, target as Block) }
    }

    override fun vcall(func: DirectFunctionPrototype, args: List<Value>, target: Label): VoidCall = mc.dfANDcf {
        require(func.returnType() == Type.Void)
        return@dfANDcf addTerminate { VoidCall.make(it, this, func, args, target as Block) }
    }

    override fun icall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, target: Label): IndirectionCall = mc.dfANDcf {
        require(func.returnType() != Type.Void)
        return@dfANDcf addTerminate { IndirectionCall.make(it, this, pointer, func, args, target as Block) }
    }

    override fun ivcall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, target: Label): IndirectionVoidCall = mc.dfANDcf {
        require(func.returnType() == Type.Void)
        return@dfANDcf addTerminate { IndirectionVoidCall.make(it, this, pointer, func, args, target as Block) }
    }

    override fun branch(target: Block): Branch = mc.cf {
        return@cf addTerminate { Branch.make(it, this, target) }
    }

    override fun branchCond(value: Value, onTrue: Label, onFalse: Label): BranchCond = mc.dfANDcf {
        return@dfANDcf addTerminate { BranchCond.make(it, this, value, onTrue as Block, onFalse as Block) }
    }

    override fun alloc(ty: NonTrivialType): Alloc = mc.df {
        return@df withOutput { it: Int -> Alloc.make(it, this, ty) }
    }

    override fun ret(returnType: Type, values: Array<Value>): Return = mc.dfANDcf {
        return@dfANDcf addTerminate { ReturnValue.make(it, this, returnType, values) }
    }

    override fun retVoid(): ReturnVoid = mc.cf {
        return@cf addTerminate{ ReturnVoid.make(it, this) }
    }

    override fun gep(source: Value, elementType: NonTrivialType, index: Value): GetElementPtr = mc.df {
        return@df withOutput { GetElementPtr.make(it, this, elementType, source, index) }
    }

    override fun gfp(source: Value, ty: AggregateType, indexes: Array<IntegerConstant>): GetFieldPtr = mc.df {
        return@df withOutput { GetFieldPtr.make(it, this, ty, source, indexes) }
    }

    override fun flag2int(value: Value, ty: IntegerType): Flag2Int = mc.df {
        return@df withOutput { Flag2Int.make(it, this, ty, value) }
    }

    override fun int2fp(value: Value, ty: FloatingPointType): Int2Float = mc.df {
        return@df withOutput { Int2Float.make(it, this, ty, value) }
    }

    override fun bitcast(value: Value, ty: PrimitiveType): Bitcast = mc.df {
        return@df withOutput { Bitcast.make(it, this, ty, value) }
    }

    override fun zext(value: Value, toType: UnsignedIntType): ZeroExtend = mc.df {
        return@df withOutput { ZeroExtend.make(it, this, toType, value) }
    }

    override fun sext(value: Value, toType: SignedIntType): SignExtend = mc.df {
        return@df withOutput { SignExtend.make(it, this, toType, value) }
    }

    override fun trunc(value: Value, toType: IntegerType): Truncate = mc.df {
        return@df withOutput { Truncate.make(it, this, toType, value) }
    }

    override fun fptrunc(value: Value, toType: FloatingPointType): FpTruncate = mc.df {
        return@df withOutput { FpTruncate.make(it, this, toType, value) }
    }

    override fun fpext(value: Value, toType: FloatingPointType): FpExtend = mc.df {
        return@df withOutput { FpExtend.make(it, this, toType, value) }
    }

    override fun fp2Int(value: Value, toType: IntegerType): FloatToInt = mc.df {
        return@df withOutput { FloatToInt.make(it,  this, toType, value) }
    }

    override fun select(cond: Value, type: IntegerType, onTrue: Value, onFalse: Value): Select = mc.df {
        return@df withOutput { Select.make(it, this, type, cond, onTrue, onFalse) }
    }

    override fun phi(incoming: List<Value>, labels: List<Label>): Phi = mc.df {
        val bbs = labels.mapTo(arrayListOf()) { it as Block }
        return@df withOutput { Phi.make(it, this, incoming[0].type() as PrimitiveType, bbs, incoming.toTypedArray()) }
    }

    override fun int2ptr(value: Value): Int2Pointer = mc.df {
        return@df withOutput { Int2Pointer.make(it, this, value) }
    }

    override fun ptr2int(value: Value, toType: IntegerType): Pointer2Int = mc.df {
        return@df withOutput { Pointer2Int.make(it, this, toType, value) }
    }

    override fun memcpy(dst: Value, src: Value, length: UnsignedIntegerConstant): Memcpy = mc.df {
        return@df withOutput { Memcpy.make(it, this, dst, src, length) }
    }

    override fun proj(tuple: Value, index: Int): Projection = mc.df {
        return@df withOutput { Projection.make(it, this, tuple, index) }
    }

    override fun switch(value: Value, default: Label, table: List<IntegerConstant>, targets: List<Label>): Switch = mc.dfANDcf {
        val resolved = arrayFrom(targets) { bb -> bb as Block }
        return@dfANDcf addTerminate { Switch.make(it, this, value, default as Block, table.toTypedArray(), resolved) }
    }

    override fun downStackFrame(callable: Callable): DownStackFrame = mc.df {
        return@df withOutput { DownStackFrame(it, this, callable) }
    }

    override fun upStackFrame(callable: Callable): UpStackFrame = mc.df {
        return@df withOutput { UpStackFrame(it, this, callable) }
    }

    override fun uncompletedPhi(ty: PrimitiveType, incoming: Value): Phi = mc.df {
        val blocks = predecessors().mapTo(arrayListOf()) { it }
        return@df withOutput { Phi.makeUncompleted(it, this, ty, incoming, blocks) } //TODO separate instruction
    }

    override fun gen(ty: NonTrivialType): Generate = mc.df {
        return@df withOutput { Generate.make(it, this, ty) }
    }

    override fun lea(source: Value): Lea = mc.df {
        return@df withOutput { Lea.make(it, this, source) }
    }

    fun uncompletedPhi(incomingType: PrimitiveType, incoming: List<Value>, labels: List<Block>): Phi = mc.df {
        val blocks = labels.mapTo(arrayListOf()) { it }
        return@df withOutput { Phi.make(it, this, incomingType, blocks, incoming.toTypedArray()) }
    }

    override fun copy(value: Value): Copy = mc.df {
        return@df withOutput { Copy.make(it, this, value) }
    }

    override fun move(dst: Generate, fromValue: Value): Move = mc.df {
        return@df withOutput { Move.make(it, this, dst, fromValue) }
    }

    override fun move(dst: Value, index: Value, src: Value): MoveByIndex = mc.df {
        return@df withOutput { MoveByIndex.make(it, this, dst, index, src) }
    }

    override fun indexedLoad(origin: Value, loadedType: PrimitiveType, index: Value): IndexedLoad = mc.df {
        return@df withOutput { IndexedLoad.make(it, this, loadedType, origin, index) }
    }

    override fun storeOnStack(destination: Value, index: Value, source: Value): StoreOnStack = mc.df {
        return@df withOutput { StoreOnStack.make(it, this, destination, index, source) }
    }

    override fun loadFromStack(origin: Value, loadedType: PrimitiveType, index: Value): LoadFromStack = mc.df {
        return@df withOutput { LoadFromStack.make(it, this, loadedType, origin, index) }
    }

    override fun leaStack(origin: Value, loadedType: PrimitiveType, index: Value): LeaStack = mc.df {
        return@df withOutput { LeaStack.make(it, this, loadedType, origin, index) }
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

    private inline fun<reified T: TerminateInstruction> addTerminate(f: (Int) -> T): T {
        val value = allocateValue()
        val instruction = f(value)

        instruction.targets().forEach { makeEdge(it) }
        append(instruction)
        return instruction
    }

    private fun allocateValue(): Int {
        val currentValue = instructionIndex
        instructionIndex += 1
        return currentValue
    }

    private inline fun<reified T: Instruction> withOutput(f: (Int) -> T): T {
        val value = allocateValue()
        val instruction = f(value)

        append(instruction)
        return instruction
    }

    private fun append(instruction: Instruction) {
        insertionStrategy.insert(instruction)
        insertionStrategy = InsertAfter(instructions.last())
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