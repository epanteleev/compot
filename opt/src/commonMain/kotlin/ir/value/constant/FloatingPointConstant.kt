package ir.value.constant

import ir.types.*


sealed interface FloatingPointConstant: PrimitiveConstant {
    override fun type(): FloatingPointType

    companion object {
        fun of(kind: FloatingPointType, value: Number): FloatingPointConstant = when (kind) {
            Type.F32 -> F32Value(value.toFloat())
            Type.F64 -> F64Value(value.toDouble())
            else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
        }
    }
}

data class F32Value(val f32: Float): FloatingPointConstant {
    override fun type(): FloatingPointType {
        return Type.F32
    }

    fun bits(): Int = f32.toBits()

    override fun toString(): String {
        return f32.toString()
    }
}

data class F64Value(val f64: Double): FloatingPointConstant {
    override fun type(): FloatingPointType {
        return Type.F64
    }

    fun bits(): Long = f64.toBits()

    override fun toString(): String {
        return f64.toString()
    }
}