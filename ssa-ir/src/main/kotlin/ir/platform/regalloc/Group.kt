package ir.platform.regalloc

import asm.x64.Operand
import ir.*
import ir.instruction.Alloc

data class Group(val values: List<LocalValue>, val precolored: Operand? = null) {
    val stackAllocGroup: Boolean by lazy {
        val isStackAlloc = values[0] is Alloc
        if (isStackAlloc) {
            assert( values.find { it !is Alloc } == null) {
                "must have only stackalloc values values=$values"
            }
        }

        isStackAlloc
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