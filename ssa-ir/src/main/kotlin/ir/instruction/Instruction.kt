package ir.instruction

import ir.*
import ir.instruction.utils.Visitor
import ir.types.Type

abstract class Instruction(protected val tp: Type, protected val operands: Array<Value>) {
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

    fun update(index: Int, new: Value) {
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

    abstract fun visit(visitor: Visitor)
    abstract fun copy(newUsages: List<Value>): Instruction
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