package ir.platform.x64.regalloc

import ir.LocalValue
import asm.x64.Operand
import common.assertion
import ir.instruction.Generate


class Group(val values: List<LocalValue>, val precolored: Operand? = null) {
    val stackAllocGroup: Boolean = values[0] is Generate

    init {
        if (stackAllocGroup) {
            assertion( values.find { it !is Generate } == null) {
                "must have only stackalloc values values=$values"
            }
        }
    }

    override fun hashCode(): Int = values.hashCode()

    fun first(): LocalValue = values[0]

    operator fun iterator(): Iterator<LocalValue> = values.iterator()

    override fun toString(): String {
        return values.joinToString(prefix = "[", separator = ",", postfix = "]")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Group

        if (values != other.values) return false
        if (precolored != other.precolored) return false
        if (stackAllocGroup != other.stackAllocGroup) return false

        return true
    }
}