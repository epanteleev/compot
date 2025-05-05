package ir.value.constant

import ir.types.*
import ir.value.constant.IntegerConstant.Companion.SIZE
import ir.value.constant.IntegerConstant.Companion.getOrCreate


sealed interface SignedIntegerConstant: IntegerConstant {
    operator fun plus(other: SignedIntegerConstant): SignedIntegerConstant
    operator fun minus(other: SignedIntegerConstant): SignedIntegerConstant
    operator fun times(other: SignedIntegerConstant): SignedIntegerConstant
    operator fun div(other: SignedIntegerConstant): SignedIntegerConstant
    operator fun rem(other: SignedIntegerConstant): SignedIntegerConstant

    infix fun shr(other: SignedIntegerConstant): SignedIntegerConstant {
        val r = value() shr other.value().toInt()
        return of(type().asType(), r)
    }

    companion object {
        fun of(kind: SignedIntType, value: Number): SignedIntegerConstant = when (kind) {
            I8Type  -> I8Value.of(value.toByte())
            I16Type -> I16Value.of(value.toShort())
            I32Type -> I32Value.of(value.toInt())
            I64Type -> I64Value.of(value.toLong())
        }
    }
}

class I8Value private constructor(val i8: Byte): SignedIntegerConstant {
    override fun type(): I8Type = I8Type
    override fun value(): Long = i8.toLong()
    override fun toString(): String = i8.toString()

    override fun plus(other: SignedIntegerConstant): SignedIntegerConstant {
        return of((i8 + other.value()).toByte())
    }

    override fun minus(other: SignedIntegerConstant): SignedIntegerConstant {
        return of((i8 - other.value()).toByte())
    }

    override fun times(other: SignedIntegerConstant): SignedIntegerConstant {
        return of((i8 * other.value()).toByte())
    }

    override fun div(other: SignedIntegerConstant): SignedIntegerConstant {
        return of((i8 / other.value()).toByte())
    }

    override fun rem(other: SignedIntegerConstant): SignedIntegerConstant {
        return of((i8 % other.value()).toByte())
    }

    companion object {
        private val table = arrayOfNulls<I8Value>(SIZE.toInt())

        fun of(i8: Byte): I8Value = getOrCreate(i8, table) { I8Value(i8) }
    }
}

class I16Value private constructor(val i16: Short): SignedIntegerConstant {
    override fun type(): SignedIntType = I16Type
    override fun value(): Long = i16.toLong()
    override fun toString(): String = i16.toString()

    override fun plus(other: SignedIntegerConstant): SignedIntegerConstant {
        return of((i16 + other.value()).toShort())
    }

    override fun minus(other: SignedIntegerConstant): SignedIntegerConstant {
        return of((i16 - other.value()).toShort())
    }

    override fun times(other: SignedIntegerConstant): SignedIntegerConstant {
        return of((i16 * other.value()).toShort())
    }

    override fun div(other: SignedIntegerConstant): SignedIntegerConstant {
        return of((i16 / other.value()).toShort())
    }

    override fun rem(other: SignedIntegerConstant): SignedIntegerConstant {
        return of((i16 % other.value()).toShort())
    }

    companion object {
        private val table = arrayOfNulls<I16Value>(SIZE.toInt())

        fun of(i16: Short): I16Value = getOrCreate(i16, table) { I16Value(i16) }
    }
}

class I32Value private constructor(val i32: Int): SignedIntegerConstant {
    override fun type(): SignedIntType = I32Type
    override fun value(): Long = i32.toLong()
    override fun toString(): String = i32.toString()

    override fun plus(other: SignedIntegerConstant): SignedIntegerConstant {
        return of(i32 + other.value().toInt())
    }

    override fun minus(other: SignedIntegerConstant): SignedIntegerConstant {
        return of(i32 - other.value().toInt())
    }

    override fun times(other: SignedIntegerConstant): SignedIntegerConstant {
        return of(i32 * other.value().toInt())
    }

    override fun div(other: SignedIntegerConstant): SignedIntegerConstant {
        return of(i32 / other.value().toInt())
    }

    override operator fun rem(other: SignedIntegerConstant): SignedIntegerConstant {
        return of(i32 % other.value().toInt())
    }

    companion object {
        private val table = arrayOfNulls<I32Value>(SIZE.toInt())

        fun of(i32: Int): I32Value = getOrCreate(i32, table) { I32Value(i32) }
        fun of(i8: Byte): I32Value = getOrCreate(i8, table) { I32Value(i8.toInt()) }
    }
}

class I64Value private constructor(val i64: Long): SignedIntegerConstant {
    override fun type(): SignedIntType = I64Type
    override fun value(): Long = i64
    override fun toString(): String = i64.toString()

    override fun plus(other: SignedIntegerConstant): SignedIntegerConstant {
        return of(i64 + other.value())
    }

    override fun minus(other: SignedIntegerConstant): SignedIntegerConstant {
        return of(i64 - other.value())
    }

    override fun times(other: SignedIntegerConstant): SignedIntegerConstant {
        return of(i64 * other.value())
    }

    override fun div(other: SignedIntegerConstant): SignedIntegerConstant {
        return of(i64 / other.value())
    }

    override operator fun rem(other: SignedIntegerConstant): SignedIntegerConstant {
        return of(i64 % other.value())
    }

    companion object {
        private val table = arrayOfNulls<I64Value>(SIZE.toInt())

        fun of(i64: Long): I64Value = getOrCreate(i64, table) { I64Value(i64) }
        fun of(i64: Int): I64Value = getOrCreate(i64, table) { I64Value(i64.toLong()) }
    }
}