package ir.value.constant

import ir.types.*
import ir.value.asValue
import ir.value.constant.IntegerConstant.Companion.SIZE
import ir.value.constant.IntegerConstant.Companion.getOrCreate


sealed interface UnsignedIntegerConstant: IntegerConstant {
    operator fun plus(other: UnsignedIntegerConstant): UnsignedIntegerConstant = when (this) {
        is U8Value  -> plus(other.asValue())
        is U16Value -> plus(other.asValue())
        is U32Value -> plus(other.asValue())
        is U64Value -> plus(other.asValue())
    }

    operator fun minus(other: UnsignedIntegerConstant): UnsignedIntegerConstant = when (this) {
        is U8Value  -> minus(other.asValue())
        is U16Value -> minus(other.asValue())
        is U32Value -> minus(other.asValue())
        is U64Value -> minus(other.asValue())
    }

    operator fun times(other: UnsignedIntegerConstant): UnsignedIntegerConstant = when (this) {
        is U8Value  -> times(other.asValue())
        is U16Value -> times(other.asValue())
        is U32Value -> times(other.asValue())
        is U64Value -> times(other.asValue())
    }

    operator fun div(other: UnsignedIntegerConstant): UnsignedIntegerConstant = when (this) {
        is U8Value  -> div(other.asValue())
        is U16Value -> div(other.asValue())
        is U32Value -> div(other.asValue())
        is U64Value -> div(other.asValue())
    }

    operator fun rem(other: UnsignedIntegerConstant): UnsignedIntegerConstant = when (this) {
        is U8Value  -> rem(other.asValue())
        is U16Value -> rem(other.asValue())
        is U32Value -> rem(other.asValue())
        is U64Value -> rem(other.asValue())
    }

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

    operator fun plus(other: U8Value): U8Value {
        return operate(u8 + other.u8)
    }

    operator fun minus(other: U8Value): U8Value {
        return operate(u8 - other.u8)
    }

    operator fun times(other: U8Value): U8Value {
        return operate(u8 * other.u8)
    }

    operator fun div(other: U8Value): U8Value {
        return operate(u8 / other.u8)
    }

    operator fun rem(other: U8Value): U8Value {
        return operate(u8 % other.u8)
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

    operator fun plus(other: U16Value): U16Value {
        return operate(u16 + other.u16)
    }

    operator fun minus(other: U16Value): U16Value {
        return operate(u16 - other.u16)
    }

    operator fun times(other: U16Value): U16Value {
        return operate(u16 * other.u16)
    }

    operator fun div(other: U16Value): U16Value {
        return operate(u16 / other.u16)
    }

    operator fun rem(other: U16Value): U16Value {
        return operate(u16 % other.u16)
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

    operator fun plus(other: U32Value): U32Value {
        return operate(u32 + other.u32)
    }

    operator fun minus(other: U32Value): U32Value {
        return operate(u32 - other.u32)
    }

    operator fun times(other: U32Value): U32Value {
        return operate(u32 * other.u32)
    }

    operator fun div(other: U32Value): U32Value {
        return operate(u32 / other.u32)
    }

    operator fun rem(other: U32Value): U32Value {
        return operate(u32 % other.u32)
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

    operator fun plus(other: U64Value): U64Value {
        return operate(u64 + other.u64)
    }

    operator fun minus(other: U64Value): U64Value {
        return operate(u64 - other.u64)
    }

    operator fun times(other: U64Value): U64Value {
        return operate(u64 * other.u64)
    }

    operator fun div(other: U64Value): U64Value {
        return operate(u64 / other.u64)
    }

    operator fun rem(other: U64Value): U64Value {
        return operate(u64 % other.u64)
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