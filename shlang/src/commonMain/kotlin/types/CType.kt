package types

import typedesc.TypeProperty


sealed class CType: TypeProperty {
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

inline fun<reified T: CType> CType.asType(): T {
    if (this !is T) {
        throw RuntimeException("Type $this is not of type ${T::class.simpleName}")
    }

    return this
}