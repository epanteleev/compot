package ir.value

import ir.types.*
import ir.instruction.Instruction


interface Value {
    fun type(): Type

    companion object {
        val UNDEF: UndefinedValue = UndefinedValue()
    }
}

inline fun<reified T> Value.asType(): T {
    val t = type()
    if (t !is T) {
        throw RuntimeException("Cannot cast $t to ${T::class}")
    }

    return t
}

inline fun<reified T: Value> Value.asValue(): T {
    val isValue = this is T
    if (!isValue) {
        throw RuntimeException("Cannot cast $this to ${T::class}")
    }

    return this as T
}