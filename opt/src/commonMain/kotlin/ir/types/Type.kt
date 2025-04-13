package ir.types

import ir.instruction.matching.TypeMatcher


data class TypeErrorException(override val message: String) : Exception(message)

sealed interface Type {
    fun isa(matcher: TypeMatcher): Boolean {
        return matcher(this)
    }
}

inline fun<reified T: Type> Type.asType(): T {
    if (this !is T) {
        throw TypeCastException("Type $this is not a ${T::class.simpleName}")
    }

    return this
}