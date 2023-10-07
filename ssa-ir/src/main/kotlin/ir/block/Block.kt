package ir.block

import ir.*
import ir.utils.TypeCheck

class Block(override val index: Int) : BlockBuilderInterface, AnyBlock {
    private val instructions = mutableListOf<Instruction>()
    private val predecessors = mutableListOf<Block>()
    private val successors = mutableListOf<Block>()

    //Fields to implement BlockBuilderInterface
    private var maxValueIndex: Int = 0
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

    override fun last(): TerminateInstruction {
        return instructions[size - 1] as TerminateInstruction
    }

    override fun begin(): Instruction {
        return instructions[0]
    }

    fun maxValueIndex(): Int {
        return maxValueIndex
    }

    val size get(): Int = instructions.size

    private fun addPredecessor(bb: Block) {
        predecessors.add(bb)
    }

    private fun addSuccessor(bb: Block) {
        successors.add(bb)
    }

    fun updateSuccessor(old: Block, new: Block) {
        val index = successors.indexOf(old)
        if (index == -1) {
            throw RuntimeException("Out of index: old=$old")
        }

        new.predecessors.add(this)
        successors[index] = new
    }

    fun removePredecessors(old: Block) {
        predecessors.remove(old)
    }

    fun insert(before: Instruction, builder: (BlockBuilderInterface) -> Value): Value {
        val beforeIndex = instructions.indexOf(before)
        assert(beforeIndex != -1) {
            "flow graph doesn't contains instruction=$before"
        }

        return insert(beforeIndex, builder)
    }

    fun insert(index: Int, builder: (BlockBuilderInterface) -> Value): Value {
        assert(index >= 0)
        indexToAppend = index
        return builder(this)
    }

    fun swap(first: Instruction, second: Instruction) {
        val firstIndex        = instructions.indexOf(first)
        val secondIndex = instructions.indexOf(second)
        assert(firstIndex != -1)
        assert(secondIndex != -1)

        val temp = instructions[firstIndex]
        instructions[firstIndex] = instructions[secondIndex]
        instructions[secondIndex] = temp
    }

    internal fun updateFlowInstruction(newInstruction: TerminateInstruction) {
        assert(instructions[size - 1] is TerminateInstruction) {
            "${instructions[size - 1]} should be terminate instruction"
        }

        instructions[size - 1] = newInstruction
    }

    fun contains(element: Instruction): Boolean {
        return instructions.contains(element)
    }

    fun isBefore(element: Instruction, before: Instruction): Boolean {
        val elementIndex = instructions.indexOf(element)
        if (elementIndex == -1) {
            return false
        }
        val beforeIndex = instructions.indexOf(before)
        if (beforeIndex == -1) {
            return false
        }

        return elementIndex < beforeIndex
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

    fun phis(): List<Phi> {
        return instructions.filterIsInstanceTo<Phi, MutableList<Phi>>(arrayListOf())
    }

    fun removeIf(filter: (Instruction) -> Boolean): Boolean {
        return instructions.removeIf { filter(it) }
    }

    override fun arithmeticUnary(op: ArithmeticUnaryOp, value: Value): Value {
        return withOutput { it: Int -> ArithmeticUnary(n(it), value.type(), op, value) }
    }

    override fun arithmeticBinary(a: Value, op: ArithmeticBinaryOp, b: Value): Value {
        if (a.type() != b.type()) {
            throw ModuleException("Operands have different types: a=${a.type()}, b=${b.type()}")
        }

        return withOutput { it: Int -> ArithmeticBinary(n(it), a.type(), a, op, b) }
    }

    override fun intCompare(a: Value, pred: IntPredicate, b: Value): Value {
        val cmp = withOutput { it: Int -> IntCompare("cmp${n(it)}", a, pred, b) }
        if (!TypeCheck.checkIntCompare(cmp as IntCompare)) {
            throw ModuleException("Operands have different types: a=${a.type()}, b=${b.type()}")
        }

        return cmp
    }

    override fun load(ptr: Value): Value {
        val load = withOutput { it: Int -> Load("v${n(it)}", ptr) }
        if (!TypeCheck.checkLoad(load as Load)) {
            throw ModuleException("Inconsistent types: ${load.dump()}")
        }

        return load
    }

    override fun store(ptr: Value, value: Value) {
        val store = Store(ptr, value)
        if (!TypeCheck.checkStore(store)) {
            throw ModuleException("Inconsistent types: ${store.dump()}")
        }

        append(store)
    }

    override fun call(func: AnyFunctionPrototype, args: ArrayList<Value>): Value {
        if (func.type() == Type.Void) {
            append(VoidCall(func, args))
            return Value.UNDEF
        }

        val call = withOutput { it: Int -> Call(n(it), func.type(), func, args) }
        if (!TypeCheck.checkCall(call as Call)) {
            throw ModuleException("Inconsistent types: ${call.dump()}")
        }

        return call
    }

    override fun branch(target: Block) {
        append(Branch(target))
    }

    override fun branchCond(value: Value, onTrue: Block, onFalse: Block) {
        append(BranchCond(value, onTrue, onFalse))
    }

    override fun stackAlloc(ty: Type, size: Long): Value {
        val alloc = withOutput { it: Int -> StackAlloc(n(it), ty, size) }
        if (!TypeCheck.checkAlloc(alloc as StackAlloc)) {
            throw ModuleException("Inconsistent types: ${alloc.dump()}")
        }

        return alloc
    }

    override fun ret(value: Value) {
        append(Return(value))
    }

    override fun gep(source: Value, index: Value): Value {
        val gep = withOutput { it: Int -> GetElementPtr(n(it), source.type(), source, index) }
        if (!TypeCheck.checkGep(gep as GetElementPtr)) {
            throw ModuleException("Inconsistent types: ${gep.dump()}")
        }

        return gep
    }

    override fun cast(value: Value, ty: Type, cast: CastType): Value {
        val castInst = withOutput { it: Int -> Cast(n(it), ty, cast, value) }
        if (!TypeCheck.checkCast(castInst as Cast)) {
            throw ModuleException("Inconsistent types: ${castInst.dump()}")
        }

        return castInst
    }

    override fun select(cond: Value, onTrue: Value, onFalse: Value): Value {
        val selectInst = withOutput { it: Int -> Select(n(it), onTrue.type(), cond, onTrue, onFalse) }
        if (!TypeCheck.checkSelect(selectInst as Select)) {
            throw ModuleException("Inconsistent types: ${selectInst.dump()}")
        }

        return selectInst
    }

    private fun n(i: Int): String {
        return "${index}x$i"
    }

    override fun phi(incoming: ArrayList<Value>, labels: ArrayList<Block>): Value {
        val phi = withOutput { it: Int -> Phi("phi${n(it)}", incoming[0].type(), labels, incoming) }

        if (!TypeCheck.checkPhi(phi as Phi)) {
            throw ModuleException("Operands have different types: labels=$labels")
        }

        return phi
    }

    override fun uncompletedPhi(incoming: Value, bb: Block): Value {
        val type = incoming.type().dereference()
        val blocks = bb.predecessors().toMutableList()
        val values = bb.predecessors().mapTo(arrayListOf()) { incoming }
        return withOutput { it: Int -> Phi("phi${n(it)}", type, blocks, values) }
    }

    override fun copy(value: Value): Value {
        return withOutput { it: Int -> Copy(n(it), value) }
    }

    private fun withOutput(f: (Int) -> ValueInstruction): Value {
        fun allocateValue(): Int {
            val currentValue = maxValueIndex
            maxValueIndex += 1
            return currentValue
        }

        val value = allocateValue()
        val instruction = f(value)

        append(instruction)
        return instruction
    }

    private fun append(instruction: Instruction) {
        fun makeEdge(to: Block) {
            addSuccessor(to)
            to.addPredecessor(this)
        }

        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

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
        fun empty(blockIndex: Int): Block {
            return Block(blockIndex)
        }
    }
}