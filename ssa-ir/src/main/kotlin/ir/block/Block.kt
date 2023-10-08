package ir.block

import ir.*
import ir.instruction.*
import ir.utils.TypeCheck

class Block(override val index: Int) : MutableBlock, AnyBlock {
    private val instructions = mutableListOf<Instruction>()
    private val predecessors = mutableListOf<Block>()
    private val successors   = mutableListOf<Block>()

    //Fields to implement BlockBuilderInterface
    private var maxValueIndex: Int = 0
    private var indexToAppend = 0
    private var insertionMode = InsertionMode.Append

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
        val last = instructions[size - 1]
        assert(last is TerminateInstruction) {
            "should be. but last=$last"
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
        insertionMode = InsertionMode.Append
        return builder(this)
    }

    fun update(before: Instruction, builder: (Instruction) -> Instruction): Instruction {
        val beforeIndex = instructions.indexOf(before)
        assert(beforeIndex != -1) {
            "flow graph doesn't contains instruction=$before"
        }

        return update(beforeIndex, builder)
    }

    fun update(index: Int, builder: (Instruction) -> Instruction): Instruction {
        assert(instructions.size > index && index >= 0)
        indexToAppend = index
        insertionMode = InsertionMode.Update
        return add(builder(instructions[index]))
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

    internal fun updateFlowInstruction(newInstruction: TerminateInstruction) {
        val last = instructions[size - 1]
        assert(last is TerminateInstruction) {
            "$last should be terminate instruction"
        }

        instructions[size - 1] = newInstruction
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
        add(Branch(target))
    }

    override fun branchCond(value: Value, onTrue: Block, onFalse: Block) {
        add(BranchCond(value, onTrue, onFalse))
    }

    override fun stackAlloc(ty: Type, size: Long): Value {
        val alloc = withOutput { it: Int -> StackAlloc(n(it), ty, size) }
        if (!TypeCheck.checkAlloc(alloc as StackAlloc)) {
            throw ModuleException("Inconsistent types: ${alloc.dump()}")
        }

        return alloc
    }

    override fun ret(value: Value) {
        add(Return(value))
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

    fun add(instruction: Instruction): Instruction {
        fun makeEdge(to: Block) {
            addSuccessor(to)
            to.addPredecessor(this)
        }
        fun updateEdge(terminateInstruction: TerminateInstruction) {
            for (idx in successors.indices) {
                successors[idx] = terminateInstruction.targets()[idx]
            }

            for (incoming in terminateInstruction.targets()) {
                val idx = incoming.predecessors.indexOf(this)
                assert(idx != -1) {
                    "should contains bb=$this in incoming=$incoming"
                }

                incoming.predecessors[idx] = this
            }
        }

        if (instruction is TerminateInstruction) {
            when (insertionMode) {
                InsertionMode.Append -> instruction.targets().forEach { makeEdge(it) }
                InsertionMode.Update -> updateEdge(instruction)
            }
        }

        append(instruction)
        return instruction
    }

    private fun n(i: Int): String {
        return "${index}x$i"
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
        when (insertionMode) {
            InsertionMode.Append -> instructions.add(indexToAppend, instruction)
            InsertionMode.Update -> instructions[indexToAppend] = instruction
        }

        insertionMode = InsertionMode.Append
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