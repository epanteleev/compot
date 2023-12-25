package ir.instruction

import ir.LocalValue
import ir.Value
import ir.types.Type

abstract class ValueInstruction(protected val identifier: String, protected val tp: Type, operands: Array<Value>):
    Instruction(operands),
    LocalValue {
    private val usedIn: MutableList<Instruction> = arrayListOf()

    internal fun addUser(instruction: Instruction) {
        usedIn.add(instruction)
    }

    internal fun killUser(instruction: Instruction) {
        usedIn.remove(instruction)
    }

    fun usedIn(): Collection<Instruction> {
        return usedIn
    }

    override fun name(): String {
        return identifier
    }

    override fun toString(): String {
        return "%$identifier"
    }

    override fun type(): Type {
        return tp
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValueInstruction

        return identifier == other.identifier && tp == other.tp
    }

    override fun hashCode(): Int {
        return identifier.hashCode() + tp.hashCode()
    }
}