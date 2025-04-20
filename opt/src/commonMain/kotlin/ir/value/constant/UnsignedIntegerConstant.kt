package ir.value.constant

import ir.types.*
import ir.value.asValue
import ir.value.constant.IntegerConstant.Companion.SIZE
import ir.value.constant.IntegerConstant.Companion.getOrCreate


sealed interface UnsignedIntegerConstant: IntegerConstant {
    operator fun plus(other: UnsignedIntegerConstant): UnsignedIntegerConstant
    operator fun minus(other: UnsignedIntegerConstant): UnsignedIntegerConstant
    operator fun times(other: UnsignedIntegerConstant): UnsignedIntegerConstant
    operator fun div(other: UnsignedIntegerConstant): UnsignedIntegerConstant
    operator fun rem(other: UnsignedIntegerConstant): UnsignedIntegerConstant
    infix fun or(other: UnsignedIntegerConstant): UnsignedIntegerConstant

    companion object {
        fun of(kind: UnsignedIntType, value: Number): UnsignedIntegerConstant = when (kind) {
            U8Type  -> U8Value.of(value.toInt().toUByte())
            U16Type -> U16Value.of(value.toInt().toUShort())
            U32Type -> U32Value.of(value.toInt().toUInt())
            U64Type -> U64Value.of(value.toLong())
        }
    }
}

class U8Value private constructor(val u8: UByte): UnsignedIntegerConstant {
    override fun type(): U8Type = U8Type
    override fun value(): Long = u8.toLong()
    override fun toString(): String = u8.toString()

    override operator fun plus(other: UnsignedIntegerConstant): U8Value {
        return operate(u8 + other.asValue<U8Value>().u8)
    }

    override operator fun minus(other: UnsignedIntegerConstant): U8Value {
        return operate(u8 - other.asValue<U8Value>().u8)
    }

    override operator fun times(other: UnsignedIntegerConstant): U8Value {
        return operate(u8 * other.asValue<U8Value>().u8)
    }

    override operator fun div(other: UnsignedIntegerConstant): U8Value {
        return operate(u8 / other.asValue<U8Value>().u8)
    }

    override fun rem(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u8 % other.asValue<U8Value>().u8)
    }

    override fun or(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        val r = u8 or other.asValue<U8Value>().u8
        return operate(r.toUInt())
    }

    companion object {
        private val table = arrayOfNulls<U8Value>(SIZE.toInt())

        private fun operate(u8: UInt): U8Value {
            return of(u8.toInt().toUByte())
        }

        fun of(u8: UByte): U8Value = getOrCreate(u8.toInt(), table) { U8Value(u8) }
    }
}

class U16Value private constructor(val u16: UShort): UnsignedIntegerConstant {
    override fun type(): U16Type = U16Type
    override fun value(): Long = u16.toUShort().toLong()
    override fun toString(): String = u16.toString()

    override fun plus(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u16 + other.asValue<U16Value>().u16)
    }

    override fun minus(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u16 - other.asValue<U16Value>().u16)
    }

    override fun times(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u16 * other.asValue<U16Value>().u16)
    }

    override operator fun div(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u16 / other.asValue<U16Value>().u16)
    }

    override fun rem(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u16 % other.asValue<U16Value>().u16)
    }

    override fun or(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        val r = u16 or other.asValue<U16Value>().u16
        return operate(r.toUInt())
    }

    companion object {
        private val table = arrayOfNulls<U16Value>(SIZE.toInt())

        private fun operate(u16: UInt): U16Value {
            return of(u16.toUShort())
        }

        fun of(u16: UShort): U16Value = getOrCreate(u16.toInt(), table) { U16Value(u16) }
    }
}

class U32Value private constructor(val u32: UInt): UnsignedIntegerConstant {
    override fun type(): U32Type = U32Type
    override fun value(): Long = u32.toUInt().toLong()
    override fun toString(): String = u32.toString()

    override fun plus(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u32 + other.asValue<U32Value>().u32)
    }

    override fun minus(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u32 - other.asValue<U32Value>().u32)
    }

    override fun times(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u32 * other.asValue<U32Value>().u32)
    }

    override operator fun div(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u32 / other.asValue<U32Value>().u32)
    }

    override fun rem(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u32 % other.asValue<U32Value>().u32)
    }

    override fun or(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        val r = u32 or other.asValue<U32Value>().u32
        return operate(r)
    }

    companion object {
        private val table = arrayOfNulls<U32Value>(SIZE.toInt())

        private fun operate(u32: UInt): U32Value {
            return of(u32)
        }

        fun of(u32: UInt): U32Value = getOrCreate(u32.toInt(), table) { U32Value(u32) }
    }
}

class U64Value private constructor(val u64: ULong): UnsignedIntegerConstant {
    override fun type(): UnsignedIntType = U64Type
    override fun value(): Long = u64.toLong()
    override fun toString(): String = u64.toString()

    override fun plus(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u64 + other.asValue<U64Value>().u64)
    }

    override fun minus(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u64 - other.asValue<U64Value>().u64)
    }

    override fun times(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u64 * other.asValue<U64Value>().u64)
    }

    override fun div(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u64 / other.asValue<U64Value>().u64)
    }

    override fun rem(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        return operate(u64 % other.asValue<U64Value>().u64)
    }

    override fun or(other: UnsignedIntegerConstant): UnsignedIntegerConstant {
        val r = u64 or other.asValue<U64Value>().u64
        return operate(r)
    }

    companion object {
        private val table = arrayOfNulls<U64Value>(SIZE.toInt())

        private fun operate(u32: ULong): U64Value {
            return of(u32)
        }

        fun of(u32: Int): U64Value = getOrCreate(u32, table) { U64Value(u32.toULong()) }
        fun of(u32: Long): U64Value = getOrCreate(u32, table) { U64Value(u32.toULong()) }
        fun of(u32: ULong): U64Value = getOrCreate(u32.toLong(), table) { U64Value(u32) }
    }
}