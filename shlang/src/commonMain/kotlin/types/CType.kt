package types

import typedesc.TypeProperty


sealed class CType: TypeProperty {
    abstract fun typename(): String
    abstract fun size(): Int
    abstract fun alignmentOf(): Int
    override fun toString(): String = typename()
}