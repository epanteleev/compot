package ir

import ir.types.*

abstract class GlobalValue(protected open val name: String): GlobalSymbol, Value {
    override fun name(): String {
        return name
    }

    override fun toString(): String {
        return "@$name"
    }

    override fun type(): PointerType {
        return Type.Ptr
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GlobalValue

        return name == other.name
    }

    abstract fun dump(): String
    abstract fun data(): String

    companion object {
        inline fun<reified T: Number> of(name: String, kind: Type, value: T): GlobalValue {
            return when (kind) {
                Type.I8  -> I8GlobalValue(name, value.toByte())
                Type.U8  -> U8GlobalValue(name, value.toByte().toUByte())
                Type.I16 -> I16GlobalValue(name, value.toShort())
                Type.U16 -> U16GlobalValue(name, value.toShort().toUShort())
                Type.I32 -> I32GlobalValue(name, value.toInt())
                Type.U32 -> U32GlobalValue(name, value.toInt().toUInt())
                Type.I64 -> I64GlobalValue(name, value.toLong())
                Type.U64 -> U64GlobalValue(name, value.toLong().toULong())
                Type.F32 -> F32GlobalValue(name, value.toFloat())
                Type.F64 -> F64GlobalValue(name, value.toDouble())
                else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }
        }
    }
}

class U8GlobalValue(override val name: String, val u8: UByte?): GlobalValue(name) {
    override fun data(): String {
        return u8?.toString().orEmpty()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${data()}"
    }
}

class I8GlobalValue(override val name: String, val i8: Byte?): GlobalValue(name) {
    override fun data(): String {
        return i8?.toString().orEmpty()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${data()}"
    }
}

class U16GlobalValue(override val name: String, val u16: UShort?): GlobalValue(name) {
    override fun data(): String {
        return u16?.toString().orEmpty()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${data()}"
    }
}

class I16GlobalValue(override val name: String, val i16: Short?): GlobalValue(name) {

    override fun data(): String {
        return i16?.toString().orEmpty()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${data()}"
    }
}

class U32GlobalValue(override val name: String, val u32: UInt?): GlobalValue(name) {
    override fun data(): String {
        return u32?.toString().orEmpty()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${data()}"
    }
}

class I32GlobalValue(override val name: String, val i32: Int?): GlobalValue(name) {
    override fun data(): String {
        return i32?.toString().orEmpty()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${data()}"
    }
}

class U64GlobalValue(override val name: String, val u64: ULong?): GlobalValue(name) {
    override fun data(): String {
        return u64?.toString().orEmpty()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${data()}"
    }
}

class I64GlobalValue(override val name: String, val i64: Long?): GlobalValue(name) {
    override fun data(): String {
        return i64?.toString().orEmpty()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${data()}"
    }
}

class F32GlobalValue(override val name: String, val f32: Float?): GlobalValue(name) {
    override fun data(): String {
        return f32?.toString().orEmpty()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${data()}"
    }
}

class F64GlobalValue(override val name: String, val f64: Double?): GlobalValue(name) {
    override fun data(): String {
        return f64?.toString().orEmpty()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${data()}"
    }
}

class StringLiteralGlobal(override val name: String, val tp: ArrayType, val string: String?): GlobalValue(name) {
    override fun data(): String {
        return "\"$string\""
    }

    override fun dump(): String {
        return "@$name = constant ${type()} ${string.orEmpty()}"
    }
}