package ir.value.constant

import ir.types.*

sealed interface NonTrivialConstant: Constant {
    abstract override fun type(): NonTrivialType

    companion object {
        fun of(kind: NonTrivialType, value: Number): NonTrivialConstant = when (kind) {
            is PrimitiveType -> PrimitiveConstant.of(kind, value)
            is AggregateType -> InitializerListValue(kind, arrayListOf(of(kind.field(0), value)))
        }
    }
}