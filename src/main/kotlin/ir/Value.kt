package ir

interface Value {
    fun type(): Type

    companion object {
        val UNDEF = UndefinedValue()
    }
}

class ArgumentValue(private val index: Int, private val tp: Type): Value {
    override fun type(): Type {
        return tp
    }

    override fun toString(): String {
        return "%$index"
    }
}

data class U8Value(val u8: Byte): Value {
    override fun type(): Type {
        return Type.U8
    }

    override fun toString(): String {
        return u8.toString()
    }
}

data class I8Value(val i8: Byte): Value {
    override fun type(): Type {
        return Type.I8
    }

    override fun toString(): String {
        return i8.toString()
    }
}

data class U16Value(val u16: Short): Value {
    override fun type(): Type {
        return Type.U16
    }

    override fun toString(): String {
        return u16.toString()
    }
}

data class I16Value(val i16: Short): Value {
    override fun type(): Type {
        return Type.I16
    }

    override fun toString(): String {
        return i16.toString()
    }
}

data class U32Value(val u32: Int): Value {
    override fun type(): Type {
        return Type.U32
    }

    override fun toString(): String {
        return u32.toString()
    }
}

data class I32Value(val i32: Int): Value {
    override fun type(): Type {
        return Type.I32
    }

    override fun toString(): String {
        return i32.toString()
    }
}

data class U64Value(val u64: Int): Value {
    override fun type(): Type {
        return Type.U64
    }

    override fun toString(): String {
        return u64.toString()
    }
}

data class I64Value(val i64: Int): Value {
    override fun type(): Type {
        return Type.I64
    }

    override fun toString(): String {
        return i64.toString()
    }
}

class UndefinedValue: Value {
    override fun type(): Type {
        return Type.UNDEF
    }

    override fun toString(): String {
        return "undef"
    }
}