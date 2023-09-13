package ir

class BasicBlock(override val index: Int) : Label(index), Collection<Instruction> {
    val instructions = mutableListOf<Instruction>()
    val predecessors = mutableListOf<BasicBlock>()
    val successors = mutableListOf<BasicBlock>()

    override val size get(): Int {
        return instructions.size
    }

    private fun addPredecessor(bb: BasicBlock) {
        predecessors.add(bb)
    }

    private fun addSuccessor(bb: BasicBlock) {
        successors.add(bb)
    }

    override fun index(): Int {
        return index
    }

    internal fun append(instruction: Instruction) {
        fun makeEdge(to: BasicBlock) {
            addSuccessor(to)
            to.addPredecessor(this)
        }

        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

        instructions.add(instruction)
    }

    internal fun prepend(instruction: ValueInstruction) {
        instructions.add(0, instruction)
    }

    fun flowInstruction(): TerminateInstruction {
        return instructions[size - 1] as TerminateInstruction
    }

    override fun contains(element: Instruction): Boolean {
        return instructions.contains(element)
    }

    override fun containsAll(elements: Collection<Instruction>): Boolean {
        return instructions.containsAll(elements)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is BlockViewer) {
            return index == other.index()
        }

        if (javaClass != other?.javaClass) return false
        other as BasicBlock

        if (index != other.index) return false
        if (instructions != other.instructions) return false
        if (predecessors != other.predecessors) return false
        return successors == other.successors
    }

    override fun hashCode(): Int {
        return index
    }

    override fun isEmpty(): Boolean {
        return instructions.isEmpty()
    }

    override operator fun iterator(): Iterator<Instruction> {
        return instructions.iterator()
    }

    fun valueInstructions(): Iterator<ValueInstruction> {
        return instructions.filterIsInstanceTo<ValueInstruction, MutableList<ValueInstruction>>(arrayListOf()).iterator()
    }

    internal fun removeIf(filter: (Instruction) -> Boolean): Boolean {
        return instructions.removeIf { filter(it) }
    }

    companion object {
        fun empty(index: Int): BasicBlock {
            return BasicBlock(index)
        }
    }
}