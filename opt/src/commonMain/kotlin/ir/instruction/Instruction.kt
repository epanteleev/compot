package ir.instruction

import ir.value.Value
import common.LListNode
import common.assertion
import ir.value.LocalValue
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


typealias Identity = Int

abstract class Instruction(protected val id: Identity, protected val owner: Block, protected val operands: Array<Value>): LListNode() {
    override fun next(): Instruction? = next as Instruction?
    override fun prev(): Instruction? = prev as Instruction?

    fun owner(): Block = owner
    fun identity(): Identity = id

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

        if (this is TerminateInstruction) {
            for (target in targets()) {
                owner.removeEdge(target)
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

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Instruction

        return id == other.id && owner.index == other.owner.index
    }

    final override fun hashCode(): Int {
        return id + owner.index
    }

    abstract fun dump(): String

    companion object {
        internal fun<T: Instruction> registerUser(user: T, vararg operands: Value): T {
            for (i in operands) {
                if (i !is LocalValue) {
                    continue
                }

                i.addUser(user)
            }

            return user
        }

        internal fun<T: Instruction> registerUser(user: T, operands: Iterator<Value>): T {
            for (i in operands) {
                if (i !is LocalValue) {
                    continue
                }

                i.addUser(user)
            }

            return user
        }
    }
}