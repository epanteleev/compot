package ir.global

import ir.value.*
import ir.types.*


sealed class GlobalConstant(protected open val name: String): GlobalSymbol {
    override fun dump(): String = if (this is AnyAggregateGlobalConstant) {
        "@$name = constant ${contentType()} ${content()}"
    } else {
        "@$name = constant ${type()} ${data()}"
    }

    override fun toString(): String = "@$name"

    override fun name(): String {
        return name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GlobalConstant

        return name == other.name
    }

    /*** Returns the internal representation of data. */
    abstract fun data(): String

    abstract fun content(): String

    abstract fun constant(): Constant

    companion object {
        fun<T: Number> of(name: String, kind: NonTrivialType, value: T): GlobalConstant {
            return when (kind) {
                Type.I8  -> I8ConstantValue(name, value.toByte())
                Type.U8  -> U8ConstantValue(name, value.toByte().toUByte())
                Type.I16 -> I16ConstantValue(name, value.toShort())
                Type.U16 -> U16ConstantValue(name, value.toShort().toUShort())
                Type.I32 -> I32ConstantValue(name, value.toInt())
                Type.U32 -> U32ConstantValue(name, value.toInt().toUInt())
                Type.I64 -> I64ConstantValue(name, value.toLong())
                Type.U64 -> U64ConstantValue(name, value.toLong().toULong())
                Type.F32 -> F32ConstantValue(name, value.toFloat())
                Type.F64 -> F64ConstantValue(name, value.toDouble())
                Type.Ptr -> if (value == 0) {
                    PointerConstant(name, value.toLong())
                } else {
                    throw RuntimeException("Cannot create pointer constant: value=$value")
                }
                else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }
        }

        fun zero(name: String, kind: NonTrivialType): GlobalConstant {
            return when (kind) {
                Type.I8  -> I8ConstantValue(name, 0)
                Type.U8  -> U8ConstantValue(name, 0.toUByte())
                Type.I16 -> I16ConstantValue(name, 0)
                Type.U16 -> U16ConstantValue(name, 0.toUShort())
                Type.I32 -> I32ConstantValue(name, 0)
                Type.U32 -> U32ConstantValue(name, 0.toUInt())
                Type.I64 -> I64ConstantValue(name, 0)
                Type.U64 -> U64ConstantValue(name, 0.toULong())
                Type.F32 -> F32ConstantValue(name, 0.0f)
                Type.F64 -> F64ConstantValue(name, 0.0)
                Type.Ptr -> PointerConstant(name, 0)
                is ArrayType -> ArrayGlobalConstant(name, InitializerListValue.zero(kind))
                is StructType -> StructGlobalConstant(name, InitializerListValue.zero(kind))
                else -> throw RuntimeException("Cannot create zero constant: kind=$kind")
            }
        }
    }
}

sealed class PrimitiveGlobalConstant(override val name: String): GlobalConstant(name)

class U8ConstantValue(override val name: String, val u8: UByte): PrimitiveGlobalConstant(name) {
    override fun data(): String {
        return u8.toString()
    }

    override fun type(): NonTrivialType = Type.U8
    override fun content(): String = data()

    override fun constant(): Constant = U8Value(u8.toByte())
}

class I8ConstantValue(override val name: String, val i8: Byte): PrimitiveGlobalConstant(name) {
    override fun data(): String {
        return i8.toString()
    }

    override fun type(): NonTrivialType = Type.I8
    override fun content(): String = data()

    override fun constant(): Constant = I8Value(i8)
}

class U16ConstantValue(override val name: String, val u16: UShort): PrimitiveGlobalConstant(name) {
    override fun data(): String {
        return u16.toString()
    }

    override fun type(): NonTrivialType = Type.U16
    override fun content(): String = data()
    override fun constant(): Constant = U16Value(u16.toShort())
}

class I16ConstantValue(override val name: String, val i16: Short): PrimitiveGlobalConstant(name) {

    override fun data(): String {
        return i16.toString()
    }

    override fun type(): NonTrivialType = Type.I16
    override fun content(): String = data()

    override fun constant(): Constant = I16Value(i16)
}

class U32ConstantValue(override val name: String, val u32: UInt): PrimitiveGlobalConstant(name) {
    override fun data(): String {
        return u32.toString()
    }

    override fun type(): NonTrivialType = Type.U32
    override fun content(): String = data()
    override fun constant(): Constant {
        return U32Value(u32.toInt())
    }
}

class I32ConstantValue(override val name: String, val i32: Int): PrimitiveGlobalConstant(name) {
    override fun data(): String {
        return i32.toString()
    }

    override fun type(): NonTrivialType = Type.I32
    override fun content(): String = data()
    override fun constant(): Constant {
        return I32Value(i32)
    }
}

class U64ConstantValue(override val name: String, val u64: ULong): PrimitiveGlobalConstant(name) {
    override fun data(): String {
        return u64.toString()
    }

    override fun type(): NonTrivialType = Type.U64
    override fun content(): String = data()
    override fun constant(): Constant {
        return U64Value(u64.toLong())
    }
}

class I64ConstantValue(override val name: String, val i64: Long): PrimitiveGlobalConstant(name) {
    override fun data(): String {
        return i64.toString()
    }

    override fun type(): NonTrivialType = Type.I64
    override fun content(): String = data()
    override fun constant(): Constant {
        return I64Value(i64)
    }
}

class F32ConstantValue(override val name: String, val f32: Float): PrimitiveGlobalConstant(name) {
    override fun data(): String {
        return f32.toBits().toString()
    }

    override fun type(): NonTrivialType = Type.F32
    override fun content(): String = f32.toString()
    override fun constant(): Constant {
        return F32Value(f32)
    }
}

class F64ConstantValue(override val name: String, val f64: Double): PrimitiveGlobalConstant(name) {
    override fun data(): String {
        return f64.toBits().toString()
    }

    override fun type(): NonTrivialType = Type.F64
    override fun content(): String = f64.toString()
    override fun constant(): Constant {
        return F64Value(f64)
    }
}

class PointerConstant(override val name: String, val value: Long): PrimitiveGlobalConstant(name) {
    override fun data(): String {
        return value.toString()
    }

    override fun type(): NonTrivialType = Type.Ptr
    override fun content(): String = data()
    override fun constant(): Constant {
        return U64Value(value)
    }
}

sealed class AnyAggregateGlobalConstant(override val name: String): GlobalConstant(name) {
    abstract fun elements(): InitializerListValue
    abstract fun contentType(): NonTrivialType
    override fun type(): NonTrivialType = Type.Ptr
}

class StringLiteralGlobalConstant(override val name: String, val tp: ArrayType, val string: String?): AnyAggregateGlobalConstant(name) {
    override fun data(): String {
        return "\"$string\""
    }

    fun isEmpty(): Boolean {
        return string.isNullOrEmpty()
    }

    override fun elements(): InitializerListValue {
        if (string == null) {
            return InitializerListValue(ArrayType(Type.I8, 0), emptyList())
        }
        return InitializerListValue(ArrayType(Type.I8, string.length), string.map { U8Value(it.code.toByte()) })
    }

    override fun content(): String = data()
    override fun contentType(): NonTrivialType = Type.Ptr
    override fun constant(): Constant {
        return StringLiteralConstant(string ?: "")
    }
}

sealed class AggregateGlobalConstant(override val name: String, val tp: NonTrivialType, protected val elements: InitializerListValue): AnyAggregateGlobalConstant(name) {
    final override fun elements(): InitializerListValue {
        return elements
    }

    final override fun data(): String {
        return elements.toString()
    }
    final override fun content(): String = data()
    final override fun constant(): Constant {
        return elements
    }
}

class ArrayGlobalConstant(name: String, elements: InitializerListValue): AggregateGlobalConstant(name, elements.type(), elements) {
    constructor(name: String, tp: ArrayType, elements: List<Constant>): this(name, InitializerListValue(tp, elements))

    override fun contentType(): ArrayType = tp as ArrayType
}

class StructGlobalConstant(name: String, elements: InitializerListValue): AggregateGlobalConstant(name, elements.type(), elements) {
    constructor(name: String, tp: StructType, elements: List<Constant>): this(name, InitializerListValue(tp, elements))

    override fun contentType(): StructType = tp as StructType
}