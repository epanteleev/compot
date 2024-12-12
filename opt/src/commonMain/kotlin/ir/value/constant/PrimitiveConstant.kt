package ir.value.constant

import ir.types.*


sealed interface PrimitiveConstant: NonTrivialConstant {
    override fun type(): PrimitiveType

    fun convertTo(toType: PrimitiveType): PrimitiveConstant = when (this) { // TODO can be removed!!!
        is I8Value -> of(toType, i8)
        is U8Value -> of(toType, u8)
        is I16Value -> of(toType, i16)
        is U16Value -> of(toType, u16)
        is I32Value -> of(toType, i32)
        is U32Value -> of(toType, u32)
        is I64Value -> of(toType, i64)
        is U64Value -> of(toType, u64)
        is F32Value -> of(toType, f32)
        is F64Value -> of(toType, f64)
        is NullValue -> of(toType, 0)
        is UndefValue -> UndefValue
        is PointerLiteral -> PointerLiteral.of(gConstant, index) //TODO remove it???
    }

    companion object {
        fun of(kind: PrimitiveType, value: Number): PrimitiveConstant = when (kind) {
            is IntegerType -> IntegerConstant.of(kind, value)
            is PtrType -> when (value.toLong()) {
                0L -> NullValue
                else -> throw IllegalArgumentException("Cannot create constant: kind=$kind, value=$value")
            }
            is FloatingPointType -> FloatingPointConstant.of(kind, value)
            is UndefType -> UndefValue
        }
    }
}