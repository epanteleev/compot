package ir.value.constant

import ir.types.*


sealed interface AggregateConstant: NonTrivialConstant

class StringLiteralConstant(val ty: ArrayType, val content: String): AggregateConstant {
    override fun type(): ArrayType {
        return ty
    }

    override fun data(): String {
        return "\"$content\""
    }

    override fun toString(): String {
        return content
    }
}

class InitializerListValue(private val type: AggregateType, val elements: List<NonTrivialConstant>): AggregateConstant, Iterable<Constant> {
    override fun type(): NonTrivialType {
        return type
    }

    override fun data(): String = toString()

    fun size(): Int = elements.size

    override fun iterator(): Iterator<Constant> {
        return elements.iterator()
    }

    override fun toString(): String {
        return elements.joinToString(", ", "{", "}")
    }

    companion object {
        fun zero(type: AggregateType): InitializerListValue {
            fun makeConstantForField(fieldType: NonTrivialType): NonTrivialConstant = when (fieldType) {
                is AggregateType -> zero(fieldType)
                is PrimitiveType -> NonTrivialConstant.of(fieldType, 0)
            }
            return InitializerListValue(type, type.fields().map { makeConstantForField(it) })
        }
    }
}