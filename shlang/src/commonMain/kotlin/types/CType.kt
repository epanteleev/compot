package types

import typedesc.TypeProperty


sealed class CType: TypeProperty {
    abstract fun typename(): String
    abstract fun size(): Int
    override fun toString(): String = typename()
}