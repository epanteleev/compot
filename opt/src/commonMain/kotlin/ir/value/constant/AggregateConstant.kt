package ir.value.constant

import ir.types.*


sealed interface AggregateConstant: NonTrivialConstant

class StringLiteralConstant(val name: String): AggregateConstant {
    override fun type(): ArrayType {
        return ArrayType(Type.U8, name.length + 1)
    }

    override fun data(): String {
        return "\"$name\""
    }

    override fun toString(): String {
        return name
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
                else -> NonTrivialConstant.of(fieldType, 0)
            }
            return InitializerListValue(type, type.fields().map { makeConstantForField(it) })
        }
    }
}