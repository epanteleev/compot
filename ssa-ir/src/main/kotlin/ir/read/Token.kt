package ir.read

import ir.Type
import ir.TypeKind

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

data class TypeToken(private val type: String, private val indirection: Int, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        val stars = "*".repeat(indirection)
        return "type '$type$stars'"
    }

    fun kind(): TypeKind {
        return matchType[type] ?: throw RuntimeException("Internal error: type=$type")
    }

    fun type(): Type {
        return when (kind()) {
            TypeKind.VOID      -> Type.Void
            TypeKind.UNDEFINED -> Type.UNDEF
            else               -> Type.of(kind(), indirection)
        }
    }

    companion object {
        private val matchType = hashMapOf(
            "u1"   to TypeKind.U1,
            "u8"   to TypeKind.U8,
            "u16"  to TypeKind.U16,
            "u32"  to TypeKind.U32,
            "u64"  to TypeKind.U64,
            "i8"   to TypeKind.I8,
            "i16"  to TypeKind.I16,
            "i32"  to TypeKind.I32,
            "i64"  to TypeKind.I16,
            "void" to TypeKind.VOID
        )
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

data class LabelToken(val name: String, override val line: Int, override val pos: Int): Token(line, pos) {
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