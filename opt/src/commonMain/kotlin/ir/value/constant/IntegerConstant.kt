package ir.value.constant

import ir.types.IntegerType
import ir.types.SignedIntType
import ir.types.Type
import ir.types.UnsignedIntType
import ir.value.constant.I64Value


sealed interface IntegerConstant: PrimitiveConstant {
    fun toInt(): Int = when (this) {
        is UnsignedIntegerConstant -> value().toInt()
        is SignedIntegerConstant   -> value().toInt()
    }

    companion object {
        fun of(kind: IntegerType, value: Number): IntegerConstant = when (kind) {
            is UnsignedIntType -> when (kind) {
                Type.U8  -> U8Value(value.toByte())
                Type.U16 -> U16Value(value.toShort())
                Type.U32 -> U32Value(value.toInt())
                Type.U64 -> U64Value(value.toLong())
                else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }
            is SignedIntType -> when (kind) {
                Type.I8  -> I8Value(value.toByte())
                Type.I16 -> I16Value(value.toShort())
                Type.I32 -> I32Value(value.toInt())
                Type.I64 -> I64Value(value.toLong())
                else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }
        }
    }
}

sealed interface SignedIntegerConstant: IntegerConstant {
    fun value(): Long
}

sealed interface UnsignedIntegerConstant: IntegerConstant {
    fun value(): ULong
}

data class U8Value(val u8: Byte): UnsignedIntegerConstant {
    override fun type(): UnsignedIntType {
        return Type.U8
    }

    override fun data(): String = toString()

    override fun value(): ULong {
        return u8.toUByte().toULong()
    }

    override fun toString(): String {
        return u8.toString()
    }
}

data class I8Value(val i8: Byte): SignedIntegerConstant {
    override fun type(): SignedIntType {
        return Type.I8
    }

    override fun data(): String = toString()

    override fun value(): Long {
        return i8.toLong()
    }

    override fun toString(): String {
        return i8.toString()
    }
}

data class U16Value(val u16: Short): UnsignedIntegerConstant {
    override fun type(): UnsignedIntType {
        return Type.U16
    }

    override fun data(): String = toString()

    override fun value(): ULong {
        return u16.toUShort().toULong()
    }

    override fun toString(): String {
        return u16.toString()
    }
}

data class I16Value(val i16: Short): SignedIntegerConstant {
    override fun type(): SignedIntType {
        return Type.I16
    }

    override fun data(): String = toString()

    override fun value(): Long {
        return i16.toLong()
    }

    override fun toString(): String {
        return i16.toString()
    }
}

data class U32Value(val u32: Int): UnsignedIntegerConstant {
    override fun type(): UnsignedIntType {
        return Type.U32
    }

    override fun data(): String = toString()

    override fun value(): ULong {
        return u32.toUInt().toULong()
    }

    override fun toString(): String {
        return u32.toString()
    }
}

data class I32Value(val i32: Int): SignedIntegerConstant {
    override fun type(): SignedIntType {
        return Type.I32
    }

    override fun data(): String = toString()

    override fun value(): Long {
        return i32.toLong()
    }

    override fun toString(): String {
        return i32.toString()
    }
}

data class U64Value(val u64: Long): UnsignedIntegerConstant {
    constructor(i64: Int): this(i64.toLong())

    override fun type(): UnsignedIntType {
        return Type.U64
    }

    override fun data(): String = toString()

    override fun value(): ULong {
        return u64.toULong()
    }

    override fun toString(): String {
        return u64.toString()
    }
}

data class I64Value(val i64: Long): SignedIntegerConstant {
    constructor(i64: Int): this(i64.toLong())

    override fun type(): SignedIntType {
        return Type.I64
    }

    override fun data(): String = toString()

    override fun value(): Long {
        return i64
    }

    override fun toString(): String {
        return i64.toString()
    }
}