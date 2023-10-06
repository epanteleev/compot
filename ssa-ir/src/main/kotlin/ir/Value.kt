package ir

interface Value {
    fun type(): Type

    companion object {
        val UNDEF = UndefinedValue()
    }
}

interface LocalValue: Value {
    fun name(): String
}

class ArgumentValue(private val index: Int, private val tp: Type): LocalValue {
    override fun name(): String {
        return "arg$index"
    }

    override fun type(): Type {
        return tp
    }

    override fun toString(): String {
        return "%$index"
    }
}

interface Constant: Value {
    companion object {
        inline fun<reified T: Number> of(kind: TypeKind, value: T): Constant {
            return when (kind) {
                TypeKind.I8  -> I8Value(value.toByte())
                TypeKind.U8  -> U8Value(value.toByte())
                TypeKind.I16 -> I16Value(value.toShort())
                TypeKind.U16 -> U16Value(value.toShort())
                TypeKind.I32 -> I32Value(value.toInt())
                TypeKind.U32 -> U32Value(value.toInt())
                TypeKind.I64 -> I64Value(value.toLong())
                TypeKind.U64 -> U64Value(value.toLong())
                TypeKind.F32 -> F32Value(value.toFloat())
                TypeKind.F64 -> F64Value(value.toDouble())
                else -> throw RuntimeException("Cannot create constant: kind=$kind, value=$value")
            }
        }
    }
}

data class U8Value(val u8: Byte): Constant {
    override fun type(): Type {
        return Type.U8
    }

    override fun toString(): String {
        return u8.toString()
    }
}

data class I8Value(val i8: Byte): Constant {
    override fun type(): Type {
        return Type.I8
    }

    override fun toString(): String {
        return i8.toString()
    }
}

data class U16Value(val u16: Short): Constant {
    override fun type(): Type {
        return Type.U16
    }

    override fun toString(): String {
        return u16.toString()
    }
}

data class I16Value(val i16: Short): Constant {
    override fun type(): Type {
        return Type.I16
    }

    override fun toString(): String {
        return i16.toString()
    }
}

data class U32Value(val u32: Int): Constant {
    override fun type(): Type {
        return Type.U32
    }

    override fun toString(): String {
        return u32.toString()
    }
}

data class I32Value(val i32: Int): Constant {
    override fun type(): Type {
        return Type.I32
    }

    override fun toString(): String {
        return i32.toString()
    }
}

data class U64Value(val u64: Long): Constant {
    override fun type(): Type {
        return Type.U64
    }

    override fun toString(): String {
        return u64.toString()
    }
}

data class I64Value(val i64: Long): Constant {
    override fun type(): Type {
        return Type.I64
    }

    override fun toString(): String {
        return i64.toString()
    }
}

data class F32Value(val f32: Float): Constant {
    override fun type(): Type {
        return Type.F32
    }

    override fun toString(): String {
        return f32.toString()
    }
}

data class F64Value(val f64: Double): Constant {
    override fun type(): Type {
        return Type.F64
    }

    override fun toString(): String {
        return f64.toString()
    }
}

class UndefinedValue: Constant, LocalValue {
    override fun name(): String {
        return toString()
    }

    override fun type(): Type {
        return Type.UNDEF
    }

    override fun toString(): String {
        return "undef"
    }
}