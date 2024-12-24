package types

import typedesc.TypeProperty


sealed class CType: TypeProperty {
    abstract fun typename(): String
    abstract fun size(): Int
    abstract fun alignmentOf(): Int
    override fun toString(): String = typename()
}

inline fun<reified T: CType> CType.asType(): T {
    if (this !is T) {
        throw RuntimeException("Type $this is not of type ${T::class.simpleName}")
    }

    return this
}