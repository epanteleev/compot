package ir.value.constant

import ir.types.*


sealed interface FloatingPointConstant: PrimitiveConstant {
    override fun type(): FloatingPointType

    companion object {
        fun of(kind: FloatingPointType, value: Number): FloatingPointConstant = when (kind) {
            F32Type -> F32Value(value.toFloat())
            F64Type -> F64Value(value.toDouble())
        }

        fun of(kind: FloatingPointType, value: UByte): FloatingPointConstant = when (kind) {
            F32Type -> F32Value(value.toFloat())
            F64Type -> F64Value(value.toDouble())
        }

        fun of(kind: FloatingPointType, value: UShort): FloatingPointConstant = when (kind) {
            F32Type -> F32Value(value.toFloat())
            F64Type -> F64Value(value.toDouble())
        }

        fun of(kind: FloatingPointType, value: UInt): FloatingPointConstant = when (kind) {
            F32Type -> F32Value(value.toFloat())
            F64Type -> F64Value(value.toDouble())
        }

        fun of(kind: FloatingPointType, value: ULong): FloatingPointConstant = when (kind) {
            F32Type -> F32Value(value.toFloat())
            F64Type -> F64Value(value.toDouble())
        }
    }
}

data class F32Value(val f32: Float): FloatingPointConstant {
    override fun type(): FloatingPointType = F32Type
    fun bits(): Int = f32.toBits()

    override fun toString(): String {
        return f32.toString()
    }
}

data class F64Value(val f64: Double): FloatingPointConstant {
    override fun type(): FloatingPointType = F64Type
    fun bits(): Long = f64.toBits()

    override fun toString(): String {
        return f64.toString()
    }
}