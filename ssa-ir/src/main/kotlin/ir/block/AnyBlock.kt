package ir.block

import ir.*

interface AnyBlock: Label {
    fun instructions(): List<Instruction>
    fun predecessors(): List<Block>
    fun successors(): List<Block>
    fun last(): TerminateInstruction
    fun begin(): Instruction
    operator fun iterator(): Iterator<Instruction>
    fun valueInstructions(): List<ValueInstruction>
}