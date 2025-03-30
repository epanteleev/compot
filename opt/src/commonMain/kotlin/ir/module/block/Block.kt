package ir.module.block

import ir.value.*
import common.assertion
import ir.instruction.*
import common.LeakedLinkedList
import ir.module.LabelResolver
import ir.module.ModificationCounter
import ir.value.constant.UndefValue


class Block private constructor(private val mc: ModificationCounter, override val index: Int): AnyBlock, Iterable<Instruction> {
    private val instructions = InstructionList()
    private val predecessors = arrayListOf<Block>()
    private val successors   = arrayListOf<Block>()

    private var instructionIndex: Int = 0

    override fun predecessors(): List<Block> {
        return predecessors
    }

    override fun successors(): List<Block> {
        return successors
    }

    override fun last(): TerminateInstruction {
        return lastOrNull() ?:
            throw IllegalStateException("Last instruction is not terminate: bb=$this, last='${instructions.lastOrNull()?.dump()}'")
    }

    override fun resolve(labelResolver: LabelResolver): Block {
        return this
    }

    fun lastOrNull(): TerminateInstruction? = instructions.lastOrNull() as? TerminateInstruction

    override fun begin(): Instruction {
        assertion(instructions.isNotEmpty()) {
            "bb=$this must have any instructions"
        }

        return instructions.first()
    }

    val size
        get(): Int = instructions.size

    private fun updateSuccessor(old: Block, new: Block): Int {
        val index = successors.indexOf(old)
        if (index == -1) {
            throw RuntimeException("Out of index: old=$old")
        }

        new.predecessors.add(this)
        successors[index] = new
        return index
    }

    private fun removePredecessors(old: Block) {
        assertion(predecessors.contains(old)) {
            "old=$old is not in bb=$this"
        }

        predecessors.remove(old)
    }

    fun instructions(fn: (Instruction) -> Unit) {
        instructions.forEach(fn)
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

    private fun updatePhi(oldSucc: Block, newSucc: Block) {
        oldSucc.phis { phi ->
            phi.incoming { oldBB, _ ->
                if (oldBB == this) newSucc else oldBB
            }
        }
    }

    fun updateCF(currentSuccessor: Block, newSuccessor: Block) = mc.cf {
        val index = updateSuccessor(currentSuccessor, newSuccessor)
        currentSuccessor.removePredecessors(this)

        val terminateInstruction = last()
        terminateInstruction.updateTarget(newSuccessor, index)

        updatePhi(currentSuccessor, newSuccessor)
    }

    override fun contains(instruction: Instruction): Boolean {
        return instructions.contains(instruction)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is BlockViewer) {
            return index == other.index
        }

        if (other == null || this::class != other::class) return false
        other as Block

        return index == other.index
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

    fun phis(fn: (Phi) -> Unit) = instructions.forEach {
        if (it !is Phi) {
            return@forEach // Assume that phi functions are in the beginning of bb.
        }

        fn(it)
    }

    fun kill(instruction: Instruction, replacement: Value): Instruction? = mc.df {
        assertion(instruction.owner() === this) {
            "instruction=$instruction is not in bb=$this"
        }

        val next = instruction.prev()
        if (instruction is UsableValue) {
            instruction.updateUsages(replacement)
        }

        val removed = remove(instruction)
        removed.destroy()
        return@df next
    }

    private fun remove(instruction: Instruction): Instruction {
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
        successors.add(to)
        to.predecessors.add(this)
    }

    internal fun removeEdge(to: Block) = mc.cf {
        successors.remove(to)
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
        kill(instruction, UndefValue)
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

private class InstructionList: LeakedLinkedList<Instruction>()