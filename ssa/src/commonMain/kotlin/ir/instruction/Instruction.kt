package ir.instruction

import ir.Value
import common.LListNode
import common.assertion
import ir.LocalValue
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block

typealias Identity = Int

abstract class Instruction(val id: Identity, val owner: Block, protected val operands: Array<Value>): LListNode() {
    override fun next(): Instruction? = next as Instruction?
    override fun prev(): Instruction? = prev as Instruction?

    fun owner(): Block = owner

    fun operands(): Array<Value> {
        return operands
    }

    fun update(newUsages: Collection<Value>) { //TODO must be removed
        for ((i, v) in newUsages.withIndex()) {
            update(i, v)
        }
    }

    fun update(closure: (Value) -> Value) {
        for ((i, v) in operands.withIndex()) {
            val newValue = closure(v)
            update(i, newValue)
        }
    }

    fun update(index: Int, new: Value) { // TODO inline class for 'index'?
        assertion(0 <= index && index < operands.size) {
            "out of range in $this"
        }

        val op = operands[index]
        if (op is LocalValue) {
            op.killUser(this)
        }

        operands[index] = new

        if (new is LocalValue) {
            new.addUser(this)
        }
    }

    fun destroy() {
        if (this is LocalValue) {
            assertion(usedIn().isEmpty()) {
                "removed useful instruction: removed=$this, users=${usedIn()}"
            }
        }

        for (idx in operands.indices) {
            val op = operands[idx]
            if (op is LocalValue) {
                op.killUser(this)
            }

            operands[idx] = Value.UNDEF
        }
    }

    abstract fun<T> visit(visitor: IRInstructionVisitor<T>): T
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
    abstract fun dump(): String

    companion object {
        internal fun<T: Instruction> registerUser(user: T, vararg instructions: Value): T {
            for (i in instructions) {
                if (i !is LocalValue) {
                    continue
                }

                i.addUser(user)
            }

            return user
        }

        internal fun<T: Instruction> registerUser(user: T, instructions: Iterator<Value>): T {
            for (i in instructions) {
                if (i !is LocalValue) {
                    continue
                }

                i.addUser(user)
            }

            return user
        }
    }
}