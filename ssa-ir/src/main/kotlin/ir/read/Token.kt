package ir.read

import ir.types.*

sealed class Token(protected open val line: Int, protected open val pos: Int) {
    abstract fun message(): String
    fun position(): String {
        return "$line:$pos"
    }
}

data class Identifier(val string: String, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "identifier '$string'"
    }
}

abstract class ValueToken(override val line: Int, override val pos: Int): Token(line, pos)

data class IntValue(val int: Long, override val line: Int, override val pos: Int): ValueToken(line, pos) {
    override fun message(): String {
        return "int value '$int'"
    }
}

data class FloatValue(val fp: Double, override val line: Int, override val pos: Int): ValueToken(line, pos) {
    override fun message(): String {
        return "float value '$fp'"
    }
}

data class ValueInstructionToken(val name: String, override val line: Int, override val pos: Int): ValueToken(line, pos) {
    override fun message(): String {
        return "value '%$name'"
    }
}

abstract class TypeToken(override val line: Int, override val pos: Int) : Token(line, pos) {
    abstract fun type(): Type
}

data class PrimitiveTypeToken(private val type: String, private val indirection: Int, override val line: Int, override val pos: Int) : TypeToken(line, pos) {
    override fun message(): String {
        val stars = "*".repeat(indirection)
        return "type '$type$stars'"
    }

    private fun kind(): Type {
        return matchType[type] ?: throw RuntimeException("Internal error: type=$type")
    }

    override fun type(): Type {
        return if (indirection == 0) {
            kind()
        } else {
            PointerType.of(kind(), indirection)
        }
    }

    inline fun<reified T: Type> asType(): T {
        val ty = type()
        if (ty !is T) {
            throw RuntimeException("actual type=$ty")
        }

        return ty
    }

    companion object {
        private val matchType = hashMapOf(
            "u1"   to Type.U1,
            "u8"   to Type.U8,
            "u16"  to Type.U16,
            "u32"  to Type.U32,
            "u64"  to Type.U64,
            "i8"   to Type.I8,
            "i16"  to Type.I16,
            "i32"  to Type.I32,
            "i64"  to Type.I64,
            "void" to Type.Void
        )
    }
}

data class ArrayTypeToken(val size: Long, val type: TypeToken, override val line: Int, override val pos: Int) : TypeToken(line, pos) {
    override fun type(): Type {
        return ArrayType(type.type(), size.toInt())
    }

    override fun message(): String {
        return "<${type.message()}, $size>"
    }
}

data class OpenParen(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'('"
    }
}

data class CloseParen(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "')'"
    }
}

data class OpenBrace(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'{'"
    }
}

data class CloseBrace(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'}'"
    }
}

data class Equal(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'='"
    }
}

data class Comma(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "','"
    }
}

data class Dot(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'.'"
    }
}

data class Define(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'define'"
    }
}

data class To(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'to'"
    }
}

data class LabelUsage(val labelName: String, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'label'"
    }
}

data class Colon(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "':'"
    }
}

data class Extern(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'extern'"
    }
}

data class LabelDefinition(val name: String, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "label '$name:'"
    }
}

data class OpenSquareBracket(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'['"
    }
}

data class CloseSquareBracket(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "']'"
    }
}

data class OpenTriangleBracket(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'<'"
    }
}

data class CloseTriangleBracket(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'>'"
    }
}

data class FunctionName(val name: String, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "function @$name"
    }
}

