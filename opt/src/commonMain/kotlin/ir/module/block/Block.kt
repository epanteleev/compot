package ir.module.block

import ir.types.*
import ir.value.*
import ir.instruction.*
import ir.instruction.lir.*
import common.LeakedLinkedList
import common.arrayFrom
import common.assertion
import ir.module.AnyFunctionPrototype
import ir.module.IndirectFunctionPrototype


class Block(override val index: Int):
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

    val size get(): Int = instructions.size

    private fun addPredecessor(bb: Block) {
        predecessors.add(bb)
    }

    private fun addSuccessor(bb: Block) {
        successors.add(bb)
    }

    private fun updateSuccessor(old: Block, new: Block) {
        val index = successors.indexOf(old)
        if (index == -1) {
            throw RuntimeException("Out of index: old=$old")
        }

        new.predecessors.add(this)
        successors[index] = new
    }

    private fun removePredecessors(old: Block) {
        predecessors.remove(old)
    }

    fun instructions(fn: (Instruction) -> Unit) {
        instructions.forEach(fn)
    }

    fun transform(fn: (Instruction) -> Instruction) {
        instructions.transform { fn(it) }
    }

    fun<T> prepend(builder: (AnyInstructionFabric) -> T): T {
        insertionStrategy = InsertBefore(null)
        return builder(this)
    }

    fun<T> insertAfter(after: Instruction, builder: (AnyInstructionFabric) -> T): T {
        insertionStrategy = InsertAfter(after)
        return builder(this)
    }

    fun<T> insertBefore(before: Instruction, builder: (AnyInstructionFabric) -> T): T {
        insertionStrategy = InsertBefore(before)
        return builder(this)
    }

    inline fun<reified T: Instruction> update(instruction: Instruction, crossinline builder: (AnyInstructionFabric) -> T): T {
        val newInstruction = insertBefore(instruction) { builder(it) }
        if (instruction is LocalValue) {
            assertion(newInstruction is LocalValue) {
                "should be local value, but newInstruction=$newInstruction"
            }
            instruction.replaceUsages(newInstruction as LocalValue)
        }
        kill(instruction)
        return newInstruction
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

    fun removeIf(filter: (Instruction) -> Boolean): Boolean {
        return instructions.removeIf { filter(it) }
    }

    fun kill(instruction: Instruction) {
        val removed = remove(instruction)
        removed.destroy()
    }

    fun remove(instruction: Instruction): Instruction {
        return instructions.remove(instruction)
    }

    override fun not(value: Value): Not {
        val valueType = value.type()
        require(valueType is IntegerType) {
            "should be integer type, but ty=$valueType"
        }

        return withOutput { Not.make(it, this, valueType, value) }
    }

    override fun neg(value: Value): Neg {
        val valueType = value.type()
        require(valueType is ArithmeticType) {
            "should be integer type, but ty=$valueType"
        }

        return withOutput { Neg.make(it, this, valueType, value) }
    }

    override fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): ArithmeticBinary {
        val ty = a.type()
        require(ty is ArithmeticType) {
            "should be arithmetic type, but ty=$ty"
        }

        return withOutput { ArithmeticBinary.make(it, this, ty, a, op, b) }
    }

    override fun tupleDiv(a: Value, b: Value): TupleDiv {
        val ty = a.type()
        require(ty is ArithmeticType) {
            "should be arithmetic type, but ty=$ty"
        }

        return withOutput { TupleDiv.make(it, this, ty, a, b) }
    }

    override fun icmp(a: Value, predicate: IntPredicate, b: Value): IntCompare {
        return withOutput { IntCompare.make(it, this, a, predicate, b) }
    }

    override fun fcmp(a: Value, predicate: FloatPredicate, b: Value): FloatCompare {
        return withOutput { FloatCompare.make(it, this, a, predicate, b) }
    }

    override fun load(loadedType: PrimitiveType, ptr: Value): Load {
        return withOutput { Load.make(it, this, loadedType, ptr) }
    }

    override fun store(ptr: Value, value: Value): Store {
        return withOutput { Store.make(it,this, ptr, value) }
    }

    override fun call(func: AnyFunctionPrototype, args: List<Value>, target: Label): Call {
        require(func.returnType() != Type.Void)
        return addTerminate { Call.make(it, this, func, args, target as Block) }
    }

    override fun tupleCall(func: AnyFunctionPrototype, args: List<Value>, target: Label): TupleCall {
        require(func.returnType() is TupleType) {
            "should be tuple type, but ty=${func.returnType()}"
        }
        return addTerminate { TupleCall.make(it, this, func, args, target as Block) }
    }

    override fun vcall(func: AnyFunctionPrototype, args: List<Value>, target: Label): VoidCall {
        require(func.returnType() == Type.Void)
        return addTerminate { VoidCall.make(it, this, func, args, target as Block) }
    }

    override fun icall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, target: Label): IndirectionCall {
        require(func.returnType() != Type.Void)
        return addTerminate { IndirectionCall.make(it, this, pointer, func, args, target as Block) }
    }

    override fun ivcall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, target: Block): IndirectionVoidCall {
        require(func.returnType() == Type.Void)
        return addTerminate { IndirectionVoidCall.make(it, this, pointer, func, args, target) }
    }

    override fun branch(target: Block): Branch {
        return addTerminate { Branch.make(it, this, target) }
    }

    override fun branchCond(value: Value, onTrue: Label, onFalse: Label): BranchCond {
        return addTerminate { BranchCond.make(it, this, value, onTrue as Block, onFalse as Block) }
    }

    override fun alloc(ty: NonTrivialType): Alloc {
        return withOutput { it: Int -> Alloc.make(it, this, ty) }
    }

    override fun ret(returnType: Type, values: Array<Value>): Return {
        return addTerminate { ReturnValue.make(it, this, returnType, values) }
    }

    override fun retVoid(): ReturnVoid {
        return addTerminate{ ReturnVoid.make(it, this) }
    }

    override fun gep(source: Value, elementType: NonTrivialType, index: Value): GetElementPtr {
        return withOutput { GetElementPtr.make(it, this, elementType, source, index) }
    }

    override fun gfp(source: Value, ty: AggregateType, indexes: Array<IntegerConstant>): GetFieldPtr {
        return withOutput { GetFieldPtr.make(it, this, ty, source, indexes) }
    }

    override fun flag2int(value: Value, ty: IntegerType): Flag2Int {
        return withOutput { Flag2Int.make(it, this, ty, value) }
    }

    override fun int2fp(value: Value, ty: FloatingPointType): Int2Float {
        return withOutput { Int2Float.make(it, this, ty, value) }
    }

    override fun bitcast(value: Value, ty: PrimitiveType): Bitcast {
        return withOutput { Bitcast.make(it, this, ty, value) }
    }

    override fun zext(value: Value, toType: UnsignedIntType): ZeroExtend {
        return withOutput { ZeroExtend.make(it, this, toType, value) }
    }

    override fun sext(value: Value, toType: SignedIntType): SignExtend {
        return withOutput { SignExtend.make(it, this, toType, value) }
    }

    override fun trunc(value: Value, toType: IntegerType): Truncate {
        return withOutput { Truncate.make(it, this, toType, value) }
    }

    override fun fptrunc(value: Value, toType: FloatingPointType): FpTruncate {
        return withOutput { FpTruncate.make(it, this, toType, value) }
    }

    override fun fpext(value: Value, toType: FloatingPointType): FpExtend {
        return withOutput { FpExtend.make(it, this, toType, value) }
    }

    override fun fp2Int(value: Value, toType: IntegerType): FloatToInt {
        return withOutput { FloatToInt.make(it,  this, toType, value) }
    }

    override fun select(cond: Value, type: PrimitiveType, onTrue: Value, onFalse: Value): Select {
        return withOutput { Select.make(it, this, type, cond, onTrue, onFalse) }
    }

    override fun phi(incoming: List<Value>, labels: List<Label>): Phi {
        val bbs = labels.mapTo(arrayListOf()) { it as Block }
        return withOutput { Phi.make(it, this, incoming[0].type() as PrimitiveType, bbs, incoming.toTypedArray()) }
    }

    override fun int2ptr(value: Value): Int2Pointer {
        return withOutput { Int2Pointer.make(it, this, value) }
    }

    override fun ptr2int(value: Value, toType: IntegerType): Pointer2Int {
        return withOutput { Pointer2Int.make(it, this, toType, value) }
    }

    override fun memcpy(dst: Value, src: Value, length: UnsignedIntegerConstant) {
        withOutput { Memcpy.make(it, this, dst, src, length) }
    }

    override fun proj(tuple: Value, index: Int): Projection {
        return withOutput { Projection.make(it, this, tuple, index) }
    }

    override fun switch(value: Value, default: Label, table: List<IntegerConstant>, targets: List<Label>): Switch {
        val resolved = arrayFrom(targets) { bb -> bb as Block }
        return addTerminate { Switch.make(it, this, value, default as Block, table.toTypedArray(), resolved) }
    }

    override fun downStackFrame(callable: Callable): DownStackFrame {
        return withOutput { DownStackFrame(it, this, callable) }
    }

    override fun upStackFrame(callable: Callable): UpStackFrame {
        return withOutput { UpStackFrame(it, this, callable) }
    }

    override fun uncompletedPhi(ty: PrimitiveType, incoming: Value): Phi {
        val blocks = predecessors().mapTo(arrayListOf()) { it }
        return withOutput { Phi.makeUncompleted(it, this, ty, incoming, blocks) }
    }

    override fun gen(ty: NonTrivialType): Generate {
        return withOutput { Generate.make(it, this, ty) }
    }

    override fun lea(generate: Value): Lea {
        return withOutput { Lea.make(it, this, generate) }
    }

    fun uncompletedPhi(incomingType: PrimitiveType, incoming: List<Value>, labels: List<Block>): Phi {
        val blocks = labels.mapTo(arrayListOf()) { it }
        return withOutput { Phi.make(it, this, incomingType, blocks, incoming.toTypedArray()) }
    }

    override fun copy(value: Value): Copy {
        return withOutput { Copy.make(it, this, value) }
    }

    override fun move(dst: Generate, fromValue: Value): Move {
        return withOutput { Move.make(it, this, dst, fromValue) }
    }

    override fun move(dst: Value, index: Value, src: Value): MoveByIndex {
        return withOutput { MoveByIndex.make(it, this, dst, index, src) }
    }

    override fun indexedLoad(origin: Value, loadedType: PrimitiveType, index: Value): IndexedLoad {
        return withOutput { IndexedLoad.make(it, this, loadedType, origin, index) }
    }

    override fun storeOnStack(destination: Value, index: Value, source: Value): StoreOnStack {
        return withOutput { StoreOnStack.make(it, this, destination, index, source) }
    }

    override fun loadFromStack(origin: Value, loadedType: PrimitiveType, index: Value): LoadFromStack {
        return withOutput { LoadFromStack.make(it, this, loadedType, origin, index) }
    }

    override fun leaStack(origin: Value, loadedType: PrimitiveType, index: Value): LeaStack {
        return withOutput { LeaStack.make(it, this, loadedType, origin, index) }
    }

    private fun makeEdge(to: Block) {
        addSuccessor(to)
        to.addPredecessor(this)
    }

    internal fun removeEdge(to: Block) {
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
        fun insertBlock(block: Block, newBlock: Block, predecessor: Block) {
            predecessor.updateSuccessor(block, newBlock)
            block.removePredecessors(predecessor)
        }

        fun empty(blockIndex: Int): Block {
            return Block(blockIndex)
        }
    }
}

private class InstructionList: LeakedLinkedList<Instruction>()