package tokenizer

interface AnyToken {
    fun line(): Int
    fun pos(): Int
    fun str(): String
}

abstract class CToken(open var next: AnyToken, open var line: Int, open var pos: Int, open val filename: String): AnyToken {
    override fun toString(): String {
        return "'${str()}'[$line: $pos]"
    }

    override fun line(): Int = line
    override fun pos(): Int = pos

    companion object {
        fun hasSpace(from: CToken, to: CToken): Boolean {
            if (from.line != to.line) {
                return true
            }
            val end = from.str().length + from.pos
            return end < to.pos
        }

        fun countSpaces(from: CToken, to: CToken): Int {
            if (from.line != to.line) {
                assert(false)
            }
            val end = from.str().length + from.pos
            return end - to.pos
        }
    }
}

data class Ident(val data: String, override var line: Int, override var pos: Int, override val filename: String): CToken(Eof, line, pos, filename) {
    override fun str(): String = data
    override fun toString(): String {
        return "'${str()}'[$line: $pos]"
    }

    companion object {
        val UNKNOWN = Ident("<unknown>",  -1, -1, "<unknown>")
    }
}

data class Punct(val data: Char, override var line: Int, override var pos: Int, override val filename: String): CToken(Eof, line, pos, filename) {
    override fun str(): String = data.toString()
    override fun toString(): String {
        return "'${str()}'[$line: $pos]"
    }
}

data class Keyword(val data: String, override var line: Int, override var pos: Int, override val filename: String): CToken(Eof, line, pos, filename) {
    override fun str(): String = data
    override fun toString(): String {
        return "'${str()}'[$line: $pos]"
    }
}

data class StringLiteral(val data: String, override var line: Int, override var pos: Int, override val filename: String): CToken(Eof, line, pos, filename) {
    override fun str(): String = data
    override fun toString(): String {
        return "'${str()}'[$line: $pos]"
    }
}

data class Numeric(val data: Number, override var line: Int, override var pos: Int, override val filename: String): CToken(Eof, line, pos, filename) {
    override fun str(): String = data.toString()
    override fun toString(): String {
        return "'${str()}'[$line: $pos]"
    }
}

data class PreprocessingNumbers(val data: String, override var line: Int, override var pos: Int, override val filename: String): CToken(Eof, line, pos, filename) {
    override fun str(): String = data
    override fun toString(): String {
        return "'${str()}'[$line: $pos]"
    }
}

object Eof: AnyToken {
    override fun line(): Int = -1
    override fun pos(): Int = -1
    override fun str(): String = "<eof>"
}