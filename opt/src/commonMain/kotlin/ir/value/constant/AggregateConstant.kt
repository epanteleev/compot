package ir.value.constant

import common.asCString
import common.quotedEscapes
import ir.types.*


sealed interface AggregateConstant: NonTrivialConstant

class StringLiteralConstant(val ty: ArrayType, val content: String): AggregateConstant {
    override fun type(): ArrayType {
        return ty
    }

    override fun toString(): String {
        return content.asCString()
    }
}

class InitializerListValue(private val type: AggregateType, val elements: List<NonTrivialConstant>): AggregateConstant, Iterable<Constant> {
    override fun type(): NonTrivialType {
        return type
    }

    fun size(): Int = elements.size

    override fun iterator(): Iterator<Constant> {
        return elements.iterator()
    }

    override fun toString(): String {
        return elements.joinToString(", ", "{", "}")
    }
}