package ir.instruction

import ir.Value
import ir.instruction.utils.IRInstructionVisitor


abstract class Instruction(protected val operands: Array<Value>) {
    fun usedInstructions(): List<ValueInstruction> {
        return operands.filterIsInstanceTo<ValueInstruction, MutableList<ValueInstruction>>(arrayListOf())
    }

    fun operands(): Array<Value> {
        return operands
    }

    fun update(newUsages: Collection<Value>) {
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
        assert(0 <= index && index < operands.size) {
            "out of range in $this"
        }

        val op = operands[index]
        if (op is ValueInstruction) {
            op.killUser(this)
        }

        operands[index] = new

        if (new is ValueInstruction) {
            new.addUser(this)
        }
    }

    fun finalize() {
        if (this is ValueInstruction) {
            assert(usedIn().isEmpty()) {
                "removed useful instruction: removed=$this, users=${usedIn()}"
            }
        }

        for (idx in operands.indices) {
            val op = operands[idx]
            if (op is ValueInstruction) {
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
                if (i !is ValueInstruction) {
                    continue
                }

                i.addUser(user)
            }

            return user
        }

        internal fun<T: Instruction> registerUser(user: T, instructions: Iterator<Value>): T {
            for (i in instructions) {
                if (i !is ValueInstruction) {
                    continue
                }

                i.addUser(user)
            }

            return user
        }
    }
}