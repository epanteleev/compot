package tokenizer


abstract class AnyToken {
    abstract fun str(): String
}

class Indent private constructor(private val spaces: Int): AnyToken() {
    private val data by lazy { " ".repeat(spaces) }

    override fun str(): String = data

    companion object {
        private val ONE = Indent(1)
        private val TWO = Indent(2)
        private val FOUR = Indent(4)

        fun of(spaces: Int): Indent {
            assert(spaces > 0) {
                "Indent should be greater than 0, but was $spaces"
            }

            when (spaces) {
                1 -> return ONE
                2 -> return TWO
                4 -> return FOUR
            }
            return Indent(spaces)
        }
    }
}

class NewLine private constructor(private val spaces: Int): AnyToken() {
    private val data by lazy { "\n".repeat(spaces) }

    override fun str(): String = data

    companion object {
        private val ONE = NewLine(1)
        private val TWO = NewLine(2)

        fun of(lines: Int): NewLine {
            assert(lines > 0) {
                "NewLine should be greater than 0, but was $lines"
            }

            when (lines) {
                1 -> return ONE
                2 -> return TWO
            }
            return NewLine(lines)
        }
    }
}

abstract class CToken(private val position: Position): AnyToken() {
    abstract fun cloneWith(pos: PreprocessedPosition): CToken

    fun line(): Int = position.line()
    fun pos(): Int = position.pos()

    fun position(): Position = position

    override fun toString(): String {
        return "'${str()}'$position'"
    }

    companion object {
        fun hasNewLine(from: CToken, to: CToken): Boolean {
            return from.position.line() != to.position.line()
        }

        fun countSpaces(from: CToken, to: CToken): Int {
            assert(!hasNewLine(from, to)) {
                "Cannot count spaces between tokens on different lines: '$from' and '$to'"
            }
            return to.position.pos() - (from.pos() + from.str().length)
        }
    }
}

class Ident(val data: String, position: Position): CToken(position) {
    override fun str(): String = data

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return Ident(data, pos)
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Ident

        return data == other.data
    }

    companion object {
        val UNKNOWN = Ident("<unknown>",  Position.UNKNOWN)
    }
}

class Punct(val data: Char, position: Position): CToken(position) {
    override fun str(): String = data.toString()

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Punct

        return data == other.data
    }

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return Punct(data, pos)
    }
}

class Keyword(val data: String, position: Position): CToken(position) {
    override fun str(): String = data

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Keyword

        return data == other.data
    }

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return Keyword(data, pos)
    }
}

class StringLiteral(val data: String, position: Position): CToken(position) {
    override fun str(): String = data

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StringLiteral

        return data == other.data
    }

    fun unquote(): String {
        return data.substring(1, data.length - 1)
    }

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return StringLiteral(data, pos)
    }
}

class Numeric(val data: Number, position: Position): CToken(position) {
    override fun str(): String = data.toString()

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Numeric

        return data == other.data
    }

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return Numeric(data, pos)
    }
}

class Eof(position: Position): CToken(position) {
    override fun str(): String = "<eof>"

    override fun hashCode(): Int {
        return 7
    }

    override fun equals(other: Any?): Boolean {
        return other is Eof
    }

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return Eof(pos)
    }
}