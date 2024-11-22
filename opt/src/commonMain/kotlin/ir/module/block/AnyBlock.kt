package ir.module.block

import ir.instruction.Instruction
import ir.instruction.TerminateInstruction


sealed interface AnyBlock : Label {
    fun predecessors(): List<Block>
    fun successors(): List<Block>
    fun last(): TerminateInstruction
    fun begin(): Instruction
    fun contains(instruction: Instruction): Boolean
    operator fun iterator(): Iterator<Instruction>
}