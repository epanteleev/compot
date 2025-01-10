package ir.value

import ir.types.*


interface Value {
    fun type(): Type
}

inline fun<reified T> Value.isa(matcher: (T) -> Boolean): Boolean {
    if (this !is T) {
        return false
    }

    return matcher(this)
}

inline fun<reified T> Value.asType(): T {
    val t = type()
    if (t !is T) {
        throw TypeCastException("Cannot cast $t to ${T::class}")
    }

    return t
}

inline fun<reified T: Value> Value.asValue(): T {
    if (this !is T) {
        throw TypeCastException("Cannot cast ${this::class} to ${T::class}")
    }

    return this
}