package ir.value

import ir.types.*


sealed interface Constant: Value {
    fun data(): String = when (this) {
        is NullValue -> "0"
        is F32Value -> f32.toBits().toString()
        is F64Value -> f64.toBits().toString()
        else -> toString()
    }

    override fun type(): NonTrivialType

    companion object {
        fun of(kind: NonTrivialType, value: Number): Constant {
            return when (kind) {
                Type.I8  -> I8Value(value.toByte())
                Type.U8  -> U8Value(value.toByte())
                Type.I16 -> I16Value(value.toShort())
                Type.U16 -> U16Value(value.toShort())
                Type.I32 -> I32Value(value.toInt())
                Type.U32 -> U32Value(value.toInt())
                Type.I64 -> I64Value(value.toLong())
                Type.U64 -> U64Value(value.toLong())
                Type.F32 -> F32Value(value.toFloat())
                Type.F64 -> F64Value(value.toDouble())
                Type.Ptr -> {
                    if (value.toLong() == 0L) {
                        NullValue.NULLPTR
                    } else {
                        throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
                    }
                }
                Type.U1  -> BoolValue.of(value.toInt() != 0)
                else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }
        }

        inline fun<reified U: Constant> valueOf(kind: NonTrivialType, value: Number): U {
            val result = of(kind, value)
            if (result !is U) {
                throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }

            return result
        }

        fun from(kind: NonTrivialType, value: Constant): Constant = when (value) {
            is I8Value -> of(kind, value.i8)
            is U8Value -> of(kind, value.u8)
            is I16Value -> of(kind, value.i16)
            is U16Value -> of(kind, value.u16)
            is I32Value -> of(kind, value.i32)
            is U32Value -> of(kind, value.u32)
            is I64Value -> of(kind, value.i64)
            is U64Value -> of(kind, value.u64)
            is F32Value -> of(kind, value.f32)
            is F64Value -> of(kind, value.f64)
            is NullValue -> of(kind, 0)
            is UndefinedValue -> Value.UNDEF
            else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
        }

        fun from(type: AggregateType, elements: List<Constant>): Constant {
            return InitializerListValue(type, elements)
        }
    }
}

class BoolValue private constructor(val bool: Boolean): Constant {
    override fun type(): NonTrivialType {
        return Type.U1
    }

    override fun toString(): String {
        return bool.toString()
    }

    companion object {
        val TRUE = BoolValue(true)
        val FALSE = BoolValue(false)

        fun of(value: Boolean): BoolValue {
            return if (value) TRUE else FALSE
        }
    }
}

class NullValue private constructor(): Constant {
    override fun type(): NonTrivialType {
        return Type.Ptr
    }

    override fun toString(): String {
        return "null"
    }

    companion object {
        val NULLPTR = NullValue()
    }
}

interface IntegerConstant: Constant {
    fun toInt(): Int {
        return when (val c = this) {
            is UnsignedIntegerConstant -> c.value().toInt()
            is SignedIntegerConstant -> c.value().toInt()
            else                        -> error("Unexpected index type")
        }
    }
}

sealed interface SignedIntegerConstant: IntegerConstant {
    fun value(): Long
}

sealed interface UnsignedIntegerConstant: IntegerConstant {
    fun value(): ULong
}

sealed interface FloatingPointConstant: Constant

data class U8Value(val u8: Byte): UnsignedIntegerConstant {
    override fun type(): UnsignedIntType {
        return Type.U8
    }

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

    override fun value(): Long {
        return i32.toLong()
    }

    override fun toString(): String {
        return i32.toString()
    }
}

data class U64Value(val u64: Long): UnsignedIntegerConstant {
    override fun type(): UnsignedIntType {
        return Type.U64
    }

    override fun value(): ULong {
        return u64.toULong()
    }

    override fun toString(): String {
        return u64.toString()
    }
}

data class I64Value(val i64: Long): SignedIntegerConstant {
    override fun type(): SignedIntType {
        return Type.I64
    }

    override fun value(): Long {
        return i64
    }

    override fun toString(): String {
        return i64.toString()
    }
}

data class F32Value(val f32: Float): FloatingPointConstant {
    override fun type(): FloatingPointType {
        return Type.F32
    }

    override fun toString(): String {
        return f32.toString()
    }
}

data class F64Value(val f64: Double): FloatingPointConstant {
    override fun type(): FloatingPointType {
        return Type.F64
    }

    override fun toString(): String {
        return f64.toString()
    }
}

object UndefinedValue: Constant {
    fun name(): String {
        return toString()
    }

    override fun type(): BottomType {
        return Type.UNDEF
    }

    override fun toString(): String {
        return "undef"
    }

    override fun hashCode(): Int {
        return -1;
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return true
    }
}

class InitializerListValue(val type: AggregateType, val elements: List<Constant>): Constant, Iterable<Constant> {
    override fun type(): NonTrivialType {
        return type
    }

    fun size(): Int = elements.size

    override fun iterator(): Iterator<Constant> {
        return elements.iterator()
    }

    override fun toString(): String {
        return elements.joinToString(", ", "{", "}")
    }

    fun linearize(): List<Constant> {
        val result = mutableListOf<Constant>()
        for (element in elements) {
            if (element is InitializerListValue) {
                result.addAll(element.linearize())
            } else {
                result.add(element)
            }
        }
        return result
    }

    companion object {
        fun zero(type: AggregateType): InitializerListValue {
            fun makeConstantForField(fieldType: NonTrivialType): Constant = when (fieldType) {
                is AggregateType -> zero(fieldType)
                else -> Constant.of(fieldType, 0)
            }
            return InitializerListValue(type, type.fields().map { makeConstantForField(it) })
        }
    }
}