package ir.value.constant

import ir.types.*
import ir.value.Value


sealed interface Constant: Value {
    fun data(): String

    override fun type(): Type

    companion object {
        fun of(kind: NonTrivialType, value: Number): Constant = when (kind) {
            is PrimitiveType -> PrimitiveConstant.of(kind, value)
            Type.U1 -> when (value.toInt()) {
                0 -> BoolValue.FALSE
                1 -> BoolValue.TRUE
                else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }
            is AggregateType -> InitializerListValue(kind, arrayListOf(NonTrivialConstant.of(kind.field(0), value)))
            else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
        }

        inline fun<reified U: Constant> valueOf(kind: NonTrivialType, value: Number): U {
            val result = of(kind, value)
            if (result !is U) {
                throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }

            return result
        }

        fun zero(kind: NonTrivialType): Constant = when (kind) {
            is PrimitiveType -> PrimitiveConstant.of(kind, 0)
            is AggregateType -> InitializerListValue.zero(kind)
            else -> throw RuntimeException("Cannot create zero constant: kind=$kind")
        }
    }
}