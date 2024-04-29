package tokenizer


abstract class CToken(private val position: Position) {
    abstract fun cloneWith(pos: PreprocessedPosition): CToken

    abstract fun str(): String
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
            val end = from.str().length + from.position.pos()
            return end - to.position.pos()
        }
    }
}

class Ident(val data: String, position: Position): CToken(position) {
    override fun str(): String = data

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return Ident(data, pos)
    }

    companion object {
        val UNKNOWN = Ident("<unknown>",  Position.UNKNOWN)
    }
}

class Punct(val data: Char, position: Position): CToken(position) {
    override fun str(): String = data.toString()

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return Punct(data, pos)
    }
}

class Keyword(val data: String, position: Position): CToken(position) {
    override fun str(): String = data

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return Keyword(data, pos)
    }
}

class StringLiteral(val data: String, position: Position): CToken(position) {
    override fun str(): String = data

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return StringLiteral(data, pos)
    }
}

class Numeric(val data: Number, position: Position): CToken(position) {
    override fun str(): String = data.toString()

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return Numeric(data, pos)
    }
}

class Eof(position: Position): CToken(position) {
    override fun str(): String = "<eof>"

    override fun cloneWith(pos: PreprocessedPosition): CToken {
        return Eof(pos)
    }
}