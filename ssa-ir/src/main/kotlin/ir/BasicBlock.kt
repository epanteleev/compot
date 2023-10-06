package ir

class BasicBlock(override val index: Int) : Label {
    private val instructions = mutableListOf<Instruction>()
    private val predecessors = mutableListOf<BasicBlock>()
    private val successors = mutableListOf<BasicBlock>()
    fun instructions(): List<Instruction> {
        return instructions
    }

    fun predecessors(): List<BasicBlock> {
        return predecessors
    }

    fun successors(): List<BasicBlock> {
        return successors
    }

    fun flowInstruction(): TerminateInstruction =
        instructions[size - 1] as TerminateInstruction

    val size get(): Int = instructions.size

    private fun addPredecessor(bb: BasicBlock) {
        predecessors.add(bb)
    }

    private fun addSuccessor(bb: BasicBlock) {
        successors.add(bb)
    }

    fun updateSuccessor(old: BasicBlock, new: BasicBlock) {
        val index = successors.indexOf(old)
        if (index == -1) {
            throw RuntimeException("Out of index: old=$old")
        }

        new.predecessors.add(this)
        successors[index] = new
    }

    fun removePredecessors(old: BasicBlock) {
        predecessors.remove(old)
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

    internal fun rawAppend(instruction: Instruction) {
        instructions.add(instruction)
    }

    internal fun appendBeforeTerminateInstruction(instruction: Instruction) {
        require(instruction !is TerminateInstruction)
        assert(instructions[size - 1] is TerminateInstruction) {
            "'${instructions[size - 1]}' should be terminate instruction"
        }

        instructions.add(instructions.size - 1, instruction)
    }

    internal fun appendBefore(element: Instruction, before: Instruction) {
        val beforeIndex = instructions.indexOf(before)
        assert(beforeIndex != -1) {
            "flow graph doesn't contains instruction=$before"
        }

        instructions.add(beforeIndex, element)
    }

    internal fun updateFlowInstruction(newInstruction: TerminateInstruction) {
        assert(instructions[size - 1] is TerminateInstruction) {
            "${instructions[size - 1]} should be terminate instruction"
        }

        instructions[size - 1] = newInstruction
    }

    internal fun prepend(instruction: ValueInstruction) {
        instructions.add(0, instruction)
    }

    fun contains(element: Instruction): Boolean {
        return instructions.contains(element)
    }

    fun containsBefore(element: Instruction, before: Instruction): Boolean {
        val elementIndex = instructions.indexOf(element)
        if (elementIndex == -1) {
            return false
        }
        val beforeIndex = instructions.indexOf(before)
        assert(beforeIndex != -1) {
            "flow graph doesn't contains instruction=$before"
        }

        return elementIndex < beforeIndex
    }

    fun containsAll(elements: Collection<Instruction>): Boolean {
        return instructions.containsAll(elements)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is BlockViewer) {
            return index == other.index
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

    fun isEmpty(): Boolean {
        return instructions.isEmpty()
    }

    operator fun iterator(): Iterator<Instruction> {
        return instructions.iterator()
    }

    fun valueInstructions(): Iterator<ValueInstruction> {
        return instructions.filterIsInstanceTo<ValueInstruction, MutableList<ValueInstruction>>(arrayListOf()).iterator()
    }

    fun phis(): Iterator<Phi> {
        return instructions.filterIsInstanceTo<Phi, MutableList<Phi>>(arrayListOf()).iterator()
    }

    internal fun removeIf(filter: (Instruction) -> Boolean): Boolean {
        return instructions.removeIf { filter(it) }
    }

    override fun toString(): String {
        return if (index == 0) {
            "entry"
        } else {
            "L$index"
        }
    }

    companion object {
        fun empty(index: Int): BasicBlock {
            return BasicBlock(index)
        }
    }
}