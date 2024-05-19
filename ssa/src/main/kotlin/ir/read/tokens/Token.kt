package ir.read.tokens


interface AnyToken {
    fun message(): String
}

sealed class Token(protected open val line: Int, protected open val pos: Int): AnyToken {
    fun position(): String {
        return "$line:$pos"
    }
}

data class Identifier(val string: String, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "identifier '$string'"
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

data class ConstantKeyword(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'constant'"
    }
}

data class GlobalKeyword(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'global'"
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

data class TypeKeyword(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'type'"
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

data class StringLiteralToken(val string: String, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "\"$string\""
    }
}