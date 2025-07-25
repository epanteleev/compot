package ir.module.block

import ir.value.*
import common.assertion
import ir.instruction.*
import common.LeakedLinkedList
import common.arrayWrapperOf
import ir.module.LabelResolver
import ir.module.ModificationCounter
import ir.value.constant.UndefValue


class Block private constructor(private val mc: ModificationCounter, index: Int): AnyBlock<Instruction>(index) {
    internal val predecessors = arrayListOf<Block>()

    private var instructionIndex: Int = 0

    override fun predecessors(): List<Block> {
        return predecessors
    }

    override fun successors(): List<Block> = arrayWrapperOf(last().targets())

    fun last(): TerminateInstruction {
        return lastOrNull() ?:
            throw IllegalStateException("Last instruction is not terminate: bb=$this, last='${instructions.lastOrNull()?.dump()}'")
    }

    override fun resolve(labelResolver: LabelResolver): Block {
        return this
    }

    fun lastOrNull(): TerminateInstruction? = instructions.lastOrNull() as? TerminateInstruction

    fun begin(): Instruction {
        assertion(instructions.isNotEmpty()) {
            "bb=$this must have any instructions"
        }

        return instructions.first()
    }

    fun transform(fn: (Instruction) -> Instruction?) {
        instructions.transform { fn(it) }
    }

    fun<T> df(closure: () -> T): T {
        return mc.df(closure)
    }

    fun<T> cf(closure: () -> T): T {
        return mc.cf(closure)
    }

    internal fun updatePhi(oldSucc: Block, newSucc: Block) {
        oldSucc.phis { phi ->
            phi.incoming { oldBB, _ ->
                if (oldBB == this) newSucc else oldBB
            }
        }
    }

    fun contains(instruction: Instruction): Boolean {
        return instructions.contains(instruction)
    }

    fun isEmpty(): Boolean {
        return instructions.isEmpty()
    }

    fun phis(fn: (Phi) -> Unit) = instructions.forEach {
        if (it !is Phi) {
            return@forEach // Assume that phi functions are in the beginning of bb.
        }

        fn(it)
    }

    internal fun remove(instruction: Instruction): Instruction {
        return instructions.remove(instruction)
    }

    fun idom(instruction: Instruction): Instruction? {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        return instruction.prev()
    }

    fun ipdom(instruction: Instruction): Instruction? {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        return instruction.next()
    }

    private fun makeEdge(to: Block) = mc.cf {
        to.predecessors.add(this)
    }

    internal fun removeEdge(to: Block) = mc.cf {
        to.predecessors.remove(this)
    }

    private fun allocateValue(): Int {
        val currentValue = instructionIndex
        instructionIndex += 1
        return currentValue
    }

    fun<T: Instruction> put(f: InstBuilder<T>): T {
        val value = allocateValue()
        val instruction = f(value, this)
        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

        instructions.addAfter(null, instruction)
        return instruction
    }

    fun<T: Instruction> putAfter(after: Instruction, f: InstBuilder<T>): T {
        assertion(after.owner() === this) {
            "after=$after is not in bb=$this"
        }

        val instruction = f(allocateValue(), this)
        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

        instructions.addAfter(after, instruction)
        return instruction
    }

    fun<T: Instruction> putBefore(before: Instruction, f: InstBuilder<T>): T {
        assertion(before.owner() === this) {
            "before=$before is not in bb=$this"
        }

        val instruction = f(allocateValue(), this)
        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

        instructions.addBefore(before, instruction)
        return instruction
    }

    fun<T: Instruction> prepend(f: InstBuilder<T>): T {
        val instruction = f(allocateValue(), this)
        if (instruction is TerminateInstruction) {
            instruction.targets().forEach { makeEdge(it) }
        }

        instructions.addBefore(null, instruction)
        return instruction
    }

    inline fun<reified T: Instruction> replace(instruction: Instruction, noinline builder: InstBuilder<T>): T {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        val newInstruction = putBefore(instruction, builder)
        if (instruction is UsableValue) {
            assertion(newInstruction is UsableValue) {
                "should be local value, but newInstruction=$newInstruction"
            }
            instruction.updateUsages(newInstruction as UsableValue)
        }
        instruction.die(UndefValue)
        return newInstruction
    }

    override fun toString(): String {
        return if (index == 0) {
            "entry"
        } else {
            "L$index"
        }
    }

    companion object {
        fun empty(mc: ModificationCounter, blockIndex: Int): Block {
            return Block(mc, blockIndex)
        }
    }
}