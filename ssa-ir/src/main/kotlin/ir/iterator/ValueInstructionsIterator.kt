package ir.iterator

import ir.*

class ValueInstructionsIterator(private val instructions: List<Instruction>): Iterator<ValueInstruction> {
    private var index = 0
    private var next: ValueInstruction?

    init {
        next = findNext()
    }

    override fun hasNext(): Boolean {
        return next != null
    }

    private fun findNext(): ValueInstruction? {
        var instruction = instructions[index]
        index += 1
        while (instruction !is ValueInstruction) {
            if (index >= instructions.size) {
                return null
            }

            instruction = instructions[index]
            index += 1
        }

        return instruction
    }

    override fun next(): ValueInstruction {
        val current = next as ValueInstruction
        next = findNext()
        return current
    }
}