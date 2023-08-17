package ir

interface Value {
    fun type(): Type

    companion object {
        val UNDEF = UndefinedValue()
    }
}

interface LocalValue: Value

class ArgumentValue(private val index: Int, private val tp: Type): LocalValue {
    override fun type(): Type {
        return tp
    }

    override fun toString(): String {
        return "%$index"
    }
}

interface Constant: Value

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

class UndefinedValue: Constant {
    override fun type(): Type {
        return Type.UNDEF
    }

    override fun toString(): String {
        return "undef"
    }
}