package ir.value.constant

import common.asCString
import ir.types.*


sealed interface AggregateConstant: NonTrivialConstant {
    fun innerType(index: Int): NonTrivialType
}

class StringLiteralConstant(private val ty: ArrayType, val content: String): AggregateConstant {
    override fun type(): ArrayType = ty
    override fun innerType(index: Int): NonTrivialType = ty.elementType()

    fun length(): Int = content.length

    override fun toString(): String = content.asCString()
}

class InitializerListValue(private val type: AggregateType, val elements: List<NonTrivialConstant>): AggregateConstant, Iterable<Constant> {
    override fun type(): NonTrivialType = type
    override fun innerType(index: Int): NonTrivialType = type.field(index)

    fun size(): Int = elements.size

    override fun iterator(): Iterator<Constant> {
        return elements.iterator()
    }

    override fun toString(): String {
        return elements.joinToString(", ", "{", "}")
    }
}