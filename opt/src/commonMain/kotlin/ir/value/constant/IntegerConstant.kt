package ir.value.constant

import ir.types.*


sealed interface IntegerConstant: PrimitiveConstant {
    fun toInt(): Int = when (this) {
        is UnsignedIntegerConstant -> value().toInt()
        is SignedIntegerConstant   -> value().toInt()
    }

    companion object {
        fun of(kind: IntegerType, value: Number): IntegerConstant = when (kind) {
            is UnsignedIntType -> UnsignedIntegerConstant.of(kind, value)
            is SignedIntType -> SignedIntegerConstant.of(kind, value)
        }
    }
}

sealed interface SignedIntegerConstant: IntegerConstant {
    fun value(): Long

    companion object {
        fun of(kind: SignedIntType, value: Number): SignedIntegerConstant = when (kind) {
            I8Type  -> I8Value(value.toByte())
            I16Type -> I16Value(value.toShort())
            I32Type -> I32Value(value.toInt())
            I64Type -> I64Value(value.toLong())
        }
    }
}

sealed interface UnsignedIntegerConstant: IntegerConstant {
    fun value(): ULong

    companion object {
        fun of(kind: UnsignedIntType, value: Number): UnsignedIntegerConstant = when (kind) {
            U8Type  -> U8Value(value.toByte())
            U16Type -> U16Value(value.toShort())
            U32Type -> U32Value(value.toInt())
            U64Type -> U64Value(value.toLong())
        }
    }
}

data class U8Value(val u8: Byte): UnsignedIntegerConstant {
    override fun type(): U8Type = U8Type

    override fun value(): ULong {
        return u8.toUByte().toULong()
    }

    override fun toString(): String {
        return u8.toString()
    }
}

data class I8Value(val i8: Byte): SignedIntegerConstant {
    override fun type(): I8Type {
        return I8Type
    }

    override fun value(): Long {
        return i8.toLong()
    }

    override fun toString(): String {
        return i8.toString()
    }
}

data class U16Value(val u16: Short): UnsignedIntegerConstant {
    override fun type(): U16Type = U16Type

    override fun value(): ULong {
        return u16.toUShort().toULong()
    }

    override fun toString(): String {
        return u16.toString()
    }
}

data class I16Value(val i16: Short): SignedIntegerConstant {
    override fun type(): SignedIntType {
        return I16Type
    }

    override fun value(): Long {
        return i16.toLong()
    }

    override fun toString(): String {
        return i16.toString()
    }
}

data class U32Value(val u32: Int): UnsignedIntegerConstant {
    override fun type(): U32Type = U32Type

    override fun value(): ULong {
        return u32.toUInt().toULong()
    }

    override fun toString(): String {
        return u32.toString()
    }
}

data class I32Value(val i32: Int): SignedIntegerConstant {
    override fun type(): SignedIntType {
        return I32Type
    }

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
        return U64Type
    }

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
        return I64Type
    }

    override fun value(): Long {
        return i64
    }

    override fun toString(): String {
        return i64.toString()
    }
}