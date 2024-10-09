package ir.value.constant

import ir.types.*


sealed interface FloatingPointConstant: PrimitiveConstant

data class F32Value(val f32: Float): FloatingPointConstant {
    override fun type(): FloatingPointType {
        return Type.F32
    }

    override fun data(): String = f32.toBits().toString()

    fun bits(): Int = f32.toBits()

    override fun toString(): String {
        return f32.toString()
    }
}

data class F64Value(val f64: Double): FloatingPointConstant {
    override fun type(): FloatingPointType {
        return Type.F64
    }

    override fun data(): String = f64.toBits().toString()

    fun bits(): Long = f64.toBits()

    override fun toString(): String {
        return f64.toString()
    }
}