package ir.module.block

import ir.AnyFunctionPrototype
import ir.IndirectFunctionPrototype
import ir.IntegerConstant
import ir.Value
import ir.instruction.*
import ir.types.*


class Block(override val index: Int, private var maxValueIndex: Int = 0) :
    AnyInstructionFabric, AnyBlock {
    private val instructions = mutableListOf<Instruction>()
    private val predecessors = mutableListOf<Block>()
    private val successors   = mutableListOf<Block>()

    private var indexToAppend = 0

    override fun instructions(): List<Instruction> {
        return instructions
    }

    override fun predecessors(): List<Block> {
        return predecessors
    }

    override fun successors(): List<Block> {
        return successors
    }

    fun hasCriticalEdgeFrom(predecessor: Block): Boolean {
        return predecessor.successors().size > 1 && predecessors().size > 1
    }

    override fun last(): TerminateInstruction {
        val last = instructions[size - 1]
        assert(last is TerminateInstruction) {
            "should be, but last=$last"
        }

        return last as TerminateInstruction
    }

    override fun begin(): Instruction {
        assert(instructions.isNotEmpty()) {
            "bb=$this must have any instructions"
        }

        return instructions[0]
    }

    val size get(): Int = instructions.size

    fun maxValueIndex(): Int {
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

    fun insert(before: Instruction, builder: (AnyInstructionFabric) -> Value): Value {
        val beforeIndex = instructions.indexOf(before)
        assert(beforeIndex != -1) {
            "flow graph doesn't contains instruction=$before"
        }

        return insert(beforeIndex, builder)
    }

    fun<T> insert(index: Int, builder: (AnyInstructionFabric) -> T): T {
        assert(index >= 0)
        indexToAppend = index
        return builder(this)
    }

    fun swap(firstIndex: Int, secondIndex: Int) {
        assert(instructions.size > firstIndex && firstIndex >= 0)
        assert(instructions.size > secondIndex && secondIndex >= 0)

        val temp = instructions[firstIndex]
        instructions[firstIndex] = instructions[secondIndex]
        instructions[secondIndex] = temp
    }

    fun indexOf(instruction: Instruction): Int {
        val index = instructions.indexOf(instruction)
        assert(index != -1) {
            "instruction=$instruction doesn't exist in $this block."
        }

        return index
    }

    fun contains(element: Instruction): Boolean {
        return instructions.contains(element)
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

    override fun valueInstructions(): List<ValueInstruction> {
        return instructions.filterIsInstanceTo<ValueInstruction, MutableList<ValueInstruction>>(arrayListOf())
    }

    fun valueInstructions(fn: (ValueInstruction) -> Unit) {
        instructions.forEach {
            if (it !is ValueInstruction) {
                return
            }

            fn(it)
        }
    }

    fun phis(): List<Phi> {
        return instructions.filterIsInstanceTo<Phi, MutableList<Phi>>(arrayListOf())
    }

    fun phis(fn: (Phi) -> Unit) {
        instructions.forEach {
            if (it !is Phi) {
                return
            }

            fn(it)
        }
    }

    fun removeIf(filter: (Instruction) -> Boolean): Boolean {
        return instructions.removeIf { filter(it) }
    }

    fun remove(instructionIndex: Int) {
        val removed = instructions.removeAt(instructionIndex)
        removed.finalize()
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

    override fun alloc(ty: Type): Alloc {
        return withOutput { it: Int -> Alloc.make(n(it), ty) }
    }

    override fun ret(value: Value) {
        add(ReturnValue.make(value))
    }

    override fun retVoid() {
        add(ReturnVoid.make())
    }

    override fun gep(source: Value, ty: PrimitiveType, index: Value): GetElementPtr {
        return withOutput { it: Int -> GetElementPtr.make(n(it), ty, source, index) }
    }

    override fun gfp(source: Value, ty: AggregateType, index: IntegerConstant): GetFieldPtr {
        return withOutput { it: Int -> GetFieldPtr.make(n(it), ty, source, index) }
    }

    override fun bitcast(value: Value, ty: PrimitiveType): Bitcast {
        return withOutput { it: Int -> Bitcast.make(n(it), ty, value) }
    }

    override fun zext(value: Value, toType: IntegerType): ZeroExtend {
        return withOutput { it: Int -> ZeroExtend.make(n(it), toType, value) }
    }

    override fun sext(value: Value, toType: IntegerType): SignExtend {
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

    override fun fptosi(value: Value, toType: SignedIntType): FloatToSigned {
        return withOutput { it: Int -> FloatToSigned.make(n(it), toType, value) }
    }

    override fun select(cond: Value, type: PrimitiveType, onTrue: Value, onFalse: Value): Select {
        return withOutput { it: Int -> Select.make(n(it), type, cond, onTrue, onFalse) }
    }

    override fun phi(incoming: List<Value>, labels: List<Block>): Phi {
        return withOutput { it: Int -> Phi.make("phi${n(it)}", incoming[0].type() as PrimitiveType, labels, incoming.toTypedArray()) }
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

    override fun gen(ty: PrimitiveType): Generate {
        return withOutput { it: Int -> Generate.make("gen${n(it)}", ty) }
    }

    fun uncompletedPhi(incoming: List<Value>, labels: List<Block>): Phi {
        return withOutput { it: Int -> Phi.make("phi${n(it)}", incoming[0].type() as PrimitiveType, labels, incoming.toTypedArray()) }
    }

    override fun copy(value: Value): Copy {
        return withOutput { it: Int -> Copy.make(n(it), value) }
    }

    override fun move(toValue: Generate, fromValue: Value) {
        append(Move.make(toValue, fromValue))
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
        instructions.add(indexToAppend, instruction)
        indexToAppend = instructions.size
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