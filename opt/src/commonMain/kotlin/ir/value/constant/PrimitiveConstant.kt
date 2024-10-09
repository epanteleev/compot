package ir.value.constant

import ir.types.*
import ir.value.Value


sealed interface PrimitiveConstant: NonTrivialConstant {
    override fun type(): PrimitiveType

    companion object {
        fun from(kind: NonTrivialType, value: PrimitiveConstant): Constant = when (value) {
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
            is PointerLiteral -> PointerLiteral(value.gConstant)
        }

        fun of(kind: NonTrivialType, value: Number): PrimitiveConstant = when (kind) {
            Type.I8  -> I8Value(value.toByte())
            Type.U8  -> U8Value(value.toByte())
            Type.I16 -> I16Value(value.toShort())
            Type.U16 -> U16Value(value.toShort())
            Type.I32 -> I32Value(value.toInt())
            Type.U32 -> U32Value(value.toInt())
            Type.I64 -> I64Value(value.toLong())
            Type.U64 -> U64Value(value.toLong())
            Type.F32 -> F32Value(value.toFloat())
            Type.F64 -> F64Value(value.toDouble())
            Type.Ptr -> when (value.toLong()) {
                0L -> NullValue.NULLPTR
                else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }
            else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
        }
    }
}