package ir.module.block

import ir.*
import ir.types.*
import ir.instruction.*


class Block(override val index: Int, private var maxValueIndex: Int = 0) : MutableBlock, AnyBlock {
    private val instructions = mutableListOf<Instruction>()
    private val predecessors = mutableListOf<Block>()
    private val successors   = mutableListOf<Block>()

    //Fields to implement BlockBuilderInterface
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

    fun insert(before: Instruction, builder: (MutableBlock) -> Value): Value {
        val beforeIndex = instructions.indexOf(before)
        assert(beforeIndex != -1) {
            "flow graph doesn't contains instruction=$before"
        }

        return insert(beforeIndex, builder)
    }

    fun insert(index: Int, builder: (MutableBlock) -> Value): Value {
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

    override fun arithmeticUnary(op: ArithmeticUnaryOp, value: Value): ArithmeticUnary {
        val valueType = value.type()
        require(valueType is ArithmeticType) {
            "should be arithmetic type, but ty=$valueType"
        }

        return withOutput { it: Int -> ArithmeticUnary.make(n(it), valueType, op, value) }
    }

    override fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): ArithmeticBinary {
        val ty = a.type()
        require(ty is ArithmeticType) {
            "should be arithmetic type, but ty=$ty"
        }

        return withOutput { it: Int -> ArithmeticBinary.make(n(it), ty, a, op, b) }
    }

    override fun intCompare(a: Value, pred: IntPredicate, b: Value): IntCompare {
        return withOutput { it: Int -> IntCompare.make("cmp${n(it)}", a, pred, b) }
    }

    override fun load(ptr: Value): Load {
        return withOutput { it: Int -> Load.make("v${n(it)}", ptr) }
    }

    override fun store(ptr: Value, value: Value) {
        val store = Store.make(ptr, value)
        append(store)
    }

    override fun call(func: AnyFunctionPrototype, args: ArrayList<Value>): Call {
        return withOutput { it: Int -> Call.make(n(it), func, args) }
    }

    override fun vcall(func: AnyFunctionPrototype, args: ArrayList<Value>) {
        require(func.type() == Type.Void)
        append(VoidCall.make(func, args))
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

    override fun gep(source: Value, index: Value): GetElementPtr {
        return withOutput { it: Int -> GetElementPtr.make(n(it), source, index) }
    }

    override fun cast(value: Value, ty: Type, cast: CastType): Cast {
        return withOutput { it: Int -> Cast.make(n(it), ty, cast, value) }
    }

    override fun select(cond: Value, onTrue: Value, onFalse: Value): Select {
        return withOutput { it: Int -> Select.make(n(it), onTrue.type(), cond, onTrue, onFalse) }
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

    override fun uncompletedPhi(incoming: Value): Phi {
        val blocks = predecessors()
        return withOutput { it: Int -> Phi.makeUncompleted("phi${n(it)}", incoming, blocks) }
    }

    fun uncompletedPhi(incoming: List<Value>, labels: List<Block>): Phi {
        return withOutput { it: Int -> Phi.make("phi${n(it)}", incoming[0].type() as PrimitiveType, labels, incoming.toTypedArray()) }
    }

    override fun copy(value: Value): Copy {
        return withOutput { it: Int -> Copy.make(n(it), value) }
    }

    fun add(instruction: Instruction): Instruction {
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