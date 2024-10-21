package ir.value.constant

import ir.types.*
import ir.value.Value


sealed interface PrimitiveConstant: NonTrivialConstant {
    override fun type(): PrimitiveType

    companion object {
        fun from(kind: PrimitiveType, value: PrimitiveConstant): Constant = when (value) {
            is I8Value -> of(kind, value.i8)
            is U8Value -> of(kind, value.u8)
            is I16Value -> of(kind, value.i16)
            is U16Value -> of(kind, value.u16)
            is I32Value -> of(kind, value.i32)
            is U32Value -> of(kind, value.u32)
            is I64Value -> of(kind, value.i64)
            is U64Value -> of(kind, value.u64)
            is F32Value -> of(kind, value.f32)
            is F64Value -> of(kind, value.f64)
            is NullValue -> of(kind, 0)
            is UndefinedValue -> Value.UNDEF
            is PointerLiteral -> PointerLiteral.of(value.gConstant, value.index) //TODO remove it???
        }

        fun of(kind: PrimitiveType, value: Number): PrimitiveConstant = when (kind) {
            is IntegerType -> IntegerConstant.of(kind, value)
            is PointerType -> when (value.toLong()) {
                0L -> NullValue.NULLPTR
                else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }
            is FloatingPointType -> when (kind) {
                Type.F32 -> F32Value(value.toFloat())
                Type.F64 -> F64Value(value.toDouble())
                else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }
            is UndefType -> Value.UNDEF
        }
    }
}