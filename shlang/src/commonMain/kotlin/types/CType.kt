package types

import tokenizer.Position
import typedesc.TypeProperty
import typedesc.TypeResolutionException


sealed class CType: TypeProperty {
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

inline fun<reified T: CType> CType.asType(where: Position): T {
    if (this !is T) {
        throw TypeResolutionException("Type $this is not of type ${T::class.simpleName}", where)
    }

    return this
}