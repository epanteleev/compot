package ir.codegen.x64.regalloc

import ir.*
import ir.instruction.StackAlloc

data class Group(val values: List<LocalValue>) {
    val hasArgument: ArgumentValue? by lazy {
        values.find { it is ArgumentValue } as ArgumentValue?
    }

    val stackAllocGroup: Boolean by lazy {
        val isStackAlloc = values[0] is StackAlloc
        if (isStackAlloc) {
            assert( values.find { it !is StackAlloc } == null) {
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