package ir.module.block

import ir.*
import ir.Value
import ir.types.*
import ir.instruction.*
import ir.instruction.lir.*
import common.LeakedLinkedList
import ir.module.AnyFunctionPrototype
import ir.module.IndirectFunctionPrototype


class Block(override val index: Int, private var maxValueIndex: Int = 0):
    AnyInstructionFabric, AnyBlock, Iterable<Instruction> {
    private val instructions = InstructionList()
    private val predecessors = arrayListOf<Block>()
    private val successors   = arrayListOf<Block>()

    private var insertionStrategy: InsertionStrategy = InsertAfter(null)

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
        return lastOrNull() as TerminateInstruction
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
        assert(instructions.isNotEmpty()) {
            "bb=$this must have any instructions"
        }

        return instructions[0]
    }

    val size get(): Int = instructions.size

    fun maxValueIndex(): Int { //TODO
        return maxValueIndex
    }

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

    fun forEachInstruction(fn: (Instruction) -> Int) {
        var i = 0
        while (i < instructions.size) {
            i += fn(instructions[i]) + 1
        }
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

    override fun contains(instruction: Instruction): Boolean {
        return instructions.contains(instruction)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is BlockViewer) {
            return index == other.index
        }

        if (javaClass != other?.javaClass) return false
        other as Block

        if (index != other.index) return false
        if (instructions != other.instructions) return false
        if (predecessors != other.predecessors) return false
        return successors == other.successors
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

    fun valueInstructions(fn: (ValueInstruction) -> Unit) {
        instructions.forEach {
            if (it !is ValueInstruction) {
                return
            }

            fn(it)
        }
    }

    fun phis(fn: (Phi) -> Unit) {
        instructions.forEach {
            if (it !is Phi) {
                return // Assume that phi functions are in the beginning of bb.
            }

            fn(it)
        }
    }

    fun removeIf(filter: (Instruction) -> Boolean): Boolean {
        return instructions.removeIf { filter(it) }
    }

    fun kill(instruction: Instruction) {
        val removed = instructions.remove(instruction)
        removed.destroy()
    }

    override fun not(value: Value): Not {
        val valueType = value.type()
        require(valueType is IntegerType) {
            "should be integer type, but ty=$valueType"
        }

        return withOutput { it: Int -> Not.make(n(it), valueType, value) }
    }

    override fun neg(value: Value): Neg {
        val valueType = value.type()
        require(valueType is ArithmeticType) {
            "should be integer type, but ty=$valueType"
        }

        return withOutput { it: Int -> Neg.make(n(it), valueType, value) }
    }

    override fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): ArithmeticBinary {
        val ty = a.type()
        require(ty is ArithmeticType) {
            "should be arithmetic type, but ty=$ty"
        }

        return withOutput { it: Int -> ArithmeticBinary.make(n(it), ty, a, op, b) }
    }

    override fun icmp(a: Value, predicate: IntPredicate, b: Value): SignedIntCompare {
        return withOutput { it: Int -> SignedIntCompare.make("cmp${n(it)}", a, predicate, b) }
    }

    override fun ucmp(a: Value, predicate: IntPredicate, b: Value): UnsignedIntCompare {
        return withOutput { it: Int -> UnsignedIntCompare.make("cmp${n(it)}", a, predicate, b) }
    }

    override fun pcmp(a: Value, predicate: IntPredicate, b: Value): PointerCompare {
        return withOutput { it: Int -> PointerCompare.make("cmp${n(it)}", a, predicate, b) }
    }

    override fun fcmp(a: Value, predicate: FloatPredicate, b: Value): FloatCompare {
        return withOutput { it: Int -> FloatCompare.make("cmp${n(it)}", a, predicate, b) }
    }

    override fun load(loadedType: PrimitiveType, ptr: Value): Load {
        return withOutput { it: Int -> Load.make("v${n(it)}", loadedType, ptr) }
    }

    override fun store(ptr: Value, value: Value) {
        val store = Store.make(ptr, value)
        append(store)
    }

    override fun call(func: AnyFunctionPrototype, args: List<Value>): Call {
        require(func.returnType() != Type.Void)
        return withOutput { it: Int -> Call.make(n(it), func, args) }
    }

    override fun vcall(func: AnyFunctionPrototype, args: List<Value>) {
        require(func.returnType() == Type.Void)
        append(VoidCall.make(func, args))
    }

    override fun icall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>): IndirectionCall {
        require(func.returnType() != Type.Void)
        return withOutput { it: Int -> IndirectionCall.make(n(it), pointer, func, args) }
    }

    override fun ivcall(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>) {
        require(func.returnType() == Type.Void)
        append(IndirectionVoidCall.make(pointer, func, args))
    }

    override fun branch(target: Block) {
        add(Branch.make(target))
    }

    override fun branchCond(value: Value, onTrue: Block, onFalse: Block) {
        add(BranchCond.make(value, onTrue, onFalse))
    }

    override fun alloc(ty: NonTrivialType): Alloc {
        return withOutput { it: Int -> Alloc.make(n(it), ty) }
    }

    override fun ret(value: Value) {
        add(ReturnValue.make(value))
    }

    override fun retVoid() {
        add(ReturnVoid.make())
    }

    override fun gep(source: Value, elementType: PrimitiveType, index: Value): GetElementPtr {
        return withOutput { it: Int -> GetElementPtr.make(n(it), elementType, source, index) }
    }

    override fun gfp(source: Value, ty: AggregateType, index: IntegerConstant): GetFieldPtr {
        return withOutput { it: Int -> GetFieldPtr.make(n(it), ty, source, index) }
    }

    override fun flag2int(value: Value, ty: IntegerType): Flag2Int {
        return withOutput { it: Int -> Flag2Int.make(n(it), ty, value) }
    }

    override fun int2fp(value: Value, ty: FloatingPointType): Int2Float {
        return withOutput { it: Int -> Int2Float.make(n(it), ty, value) }
    }

    override fun bitcast(value: Value, ty: PrimitiveType): Bitcast {
        return withOutput { it: Int -> Bitcast.make(n(it), ty, value) }
    }

    override fun zext(value: Value, toType: UnsignedIntType): ZeroExtend {
        return withOutput { it: Int -> ZeroExtend.make(n(it), toType, value) }
    }

    override fun sext(value: Value, toType: SignedIntType): SignExtend {
        return withOutput { it: Int -> SignExtend.make(n(it), toType, value) }
    }

    override fun trunc(value: Value, toType: IntegerType): Truncate {
        return withOutput { it: Int -> Truncate.make(n(it), toType, value) }
    }

    override fun fptrunc(value: Value, toType: FloatingPointType): FpTruncate {
        return withOutput { it: Int -> FpTruncate.make(n(it), toType, value) }
    }

    override fun fpext(value: Value, toType: FloatingPointType): FpExtend {
        return withOutput { it: Int -> FpExtend.make(n(it), toType, value) }
    }

    override fun fp2Int(value: Value, toType: IntegerType): FloatToInt {
        return withOutput { it: Int -> FloatToInt.make(n(it), toType, value) }
    }

    override fun select(cond: Value, type: PrimitiveType, onTrue: Value, onFalse: Value): Select {
        return withOutput { it: Int -> Select.make(n(it), type, cond, onTrue, onFalse) }
    }

    override fun phi(incoming: List<Value>, labels: List<Label>): Phi {
        val bbs = labels.map { it as Block }
        return withOutput { it: Int -> Phi.make("phi${n(it)}", incoming[0].type() as PrimitiveType, bbs, incoming.toTypedArray()) }
    }

    override fun int2ptr(value: Value): Int2Pointer {
        return withOutput { it: Int -> Int2Pointer.make(n(it), value) }
    }

    override fun ptr2int(value: Value, toType: IntegerType): Pointer2Int {
        return withOutput { it: Int -> Pointer2Int.make(n(it), toType, value) }
    }

    override fun memcpy(dst: Value, src: Value, length: UnsignedIntegerConstant) {
        add(Memcpy.make(dst, src, length))
    }

    override fun downStackFrame(callable: Callable) {
        add(DownStackFrame(callable))
    }

    override fun upStackFrame(callable: Callable) {
        add(UpStackFrame(callable))
    }

    override fun uncompletedPhi(ty: PrimitiveType, incoming: Value): Phi {
        val blocks = predecessors()
        return withOutput { it: Int -> Phi.makeUncompleted("phi${n(it)}", ty, incoming, blocks) }
    }

    override fun gen(ty: NonTrivialType): Generate {
        return withOutput { it: Int -> Generate.make("gen${n(it)}", ty) }
    }

    override fun lea(generate: Value): Lea {
        return withOutput { it: Int -> Lea.make("lea${n(it)}", generate) }
    }

    fun uncompletedPhi(incomingType: PrimitiveType, incoming: List<Value>, labels: List<Block>): Phi {
        return withOutput { it: Int -> Phi.make("phi${n(it)}", incomingType, labels, incoming.toTypedArray()) }
    }

    override fun copy(value: Value): Copy {
        return withOutput { it: Int -> Copy.make(n(it), value) }
    }

    override fun move(dst: Generate, fromValue: Value) {
        append(Move.make(dst, fromValue))
    }

    override fun move(dst: Value, base: Value, index: Value) {
        add(MoveByIndex.make(dst, base, index))
    }

    override fun indexedLoad(origin: Value, loadedType: PrimitiveType, index: Value): IndexedLoad {
        return withOutput { it: Int -> IndexedLoad.make(n(it), loadedType, origin, index) }
    }

    override fun storeOnStack(destination: Value, index: Value, source: Value) {
        add(StoreOnStack.make(destination, index, source))
    }

    override fun loadFromStack(origin: Value, loadedType: PrimitiveType, index: Value): LoadFromStack {
        return withOutput { it: Int -> LoadFromStack.make(n(it), loadedType, origin, index) }
    }

    override fun leaStack(origin: Value, loadedType: PrimitiveType, index: Value): LeaStack {
        return withOutput { it: Int -> LeaStack.make(n(it), loadedType, origin, index) }
    }


    fun add(instruction: Instruction): Instruction { //TODO simplify???
        fun makeEdge(to: Block) {
            addSuccessor(to)
            to.addPredecessor(this)
        }

        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

        append(instruction)
        return instruction
    }

    private fun n(i: Int): String {
        return "${index}x$i"
    }

    private fun allocateValue(): Int {
        val currentValue = maxValueIndex
        maxValueIndex += 1
        return currentValue
    }

    private inline fun<reified T: ValueInstruction> withOutput(f: (Int) -> T): T {
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

        fun empty(blockIndex: Int, maxValueIndex: Int): Block {
            return Block(blockIndex, maxValueIndex)
        }
    }
}

private class InstructionList(): LeakedLinkedList<Instruction>()