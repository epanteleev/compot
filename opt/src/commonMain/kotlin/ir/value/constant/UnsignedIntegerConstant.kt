package ir.value.constant

import ir.types.*
import ir.value.constant.IntegerConstant.Companion.SIZE
import ir.value.constant.IntegerConstant.Companion.getOrCreate


sealed interface UnsignedIntegerConstant: IntegerConstant {
    fun value(): ULong

    companion object {
        fun of(kind: UnsignedIntType, value: Number): UnsignedIntegerConstant = when (kind) {
            U8Type  -> U8Value.of(value.toByte())
            U16Type -> U16Value.of(value.toShort())
            U32Type -> U32Value.of(value.toInt())
            U64Type -> U64Value.of(value.toLong())
        }
    }
}

class U8Value private constructor(val u8: Byte): UnsignedIntegerConstant {
    override fun type(): U8Type = U8Type
    override fun value(): ULong = u8.toUByte().toULong()
    override fun toString(): String = u8.toString()

    companion object {
        private val table = arrayOfNulls<U8Value>(SIZE.toInt())

        fun of(u8: Byte): U8Value = getOrCreate(u8, table) { U8Value(u8) }
    }
}

class U16Value private constructor(val u16: Short): UnsignedIntegerConstant {
    override fun type(): U16Type = U16Type
    override fun value(): ULong = u16.toUShort().toULong()
    override fun toString(): String = u16.toString()

    companion object {
        private val table = arrayOfNulls<U16Value>(SIZE.toInt())

        fun of(u16: Short): U16Value = getOrCreate(u16, table) { U16Value(u16) }
    }
}

class U32Value private constructor(val u32: Int): UnsignedIntegerConstant {
    override fun type(): U32Type = U32Type
    override fun value(): ULong = u32.toUInt().toULong()
    override fun toString(): String = u32.toString()

    companion object {
        private val table = arrayOfNulls<U32Value>(SIZE.toInt())

        fun of(u32: Int): U32Value = getOrCreate(u32, table) { U32Value(u32) }
    }
}

class U64Value private constructor(val u64: Long): UnsignedIntegerConstant {
    override fun type(): UnsignedIntType = U64Type

    override fun value(): ULong = u64.toULong()

    override fun toString(): String = u64.toString()

    companion object {
        private val table = arrayOfNulls<U64Value>(SIZE.toInt())

        fun of(u32: Int): U64Value = getOrCreate(u32, table) { U64Value(u32.toLong()) }
        fun of(u32: Long): U64Value = getOrCreate(u32, table) { U64Value(u32) }
    }
}