package ir.platform.x64.regalloc

import ir.LocalValue
import asm.x64.Operand
import ir.instruction.Generate


data class Group(val values: List<LocalValue>, val precolored: Operand? = null) {
    val stackAllocGroup: Boolean = values[0] is Generate

    init {
        if (stackAllocGroup) {
            assert( values.find { it !is Generate } == null) {
                "must have only stackalloc values values=$values"
            }
        }
    }

    fun first(): LocalValue {
        return values[0]
    }

    operator fun iterator(): Iterator<LocalValue> {
        return values.iterator()
    }

    override fun toString(): String {
        return values.joinToString(prefix = "[", separator = ",", postfix = "]")
    }
}