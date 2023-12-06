package ir.platform.regalloc

import asm.x64.Operand
import ir.LocalValue
import ir.instruction.Alloc

data class Group(val values: List<LocalValue>, val precolored: Operand? = null) {
    val stackAllocGroup: Boolean

    init {
        stackAllocGroup = values[0] is Alloc
        if (stackAllocGroup) {
            assert( values.find { it !is Alloc } == null) {
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