package ir.instruction

import ir.*
import ir.types.Type


abstract class ValueInstruction(protected val identifier: String, protected val tp: Type, operands: Array<Value>):
    Instruction(operands),
    LocalValue {
    private var usedIn: MutableList<Instruction> = arrayListOf()

    internal fun addUser(instruction: Instruction) {
        usedIn.add(instruction)
    }

    internal fun killUser(instruction: Instruction) {
        usedIn.remove(instruction)
    }

    fun usedIn(): List<Instruction> {
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

    companion object {
        private val EMPTY_USED_IN = arrayListOf<Instruction>()

        fun replaceUsages(inst: ValueInstruction, toValue: Value) {
            val usedIn = inst.usedIn
            inst.usedIn = EMPTY_USED_IN
            for (user in usedIn) {
                for ((idxUse, use) in user.operands().withIndex()) {
                    if (use != inst) {
                        continue
                    }

                    user.update(idxUse, toValue)
                }
            }
        }
    }
}