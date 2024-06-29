package tokenizer

import common.LListNode
import common.assertion


abstract class AnyToken: LListNode() {
    abstract fun str(): String

    abstract fun cloneWith(pos: Position): AnyToken

    override fun prev(): AnyToken? {
        return super.prev() as AnyToken?
    }

    override fun next(): AnyToken? {
        return super.next() as AnyToken?
    }
}

abstract class PreprocessorGuard(val filename: String, val includeLevel: Int): AnyToken()

class EnterIncludeGuard(filename: String, includeLevel: Int): PreprocessorGuard(filename, includeLevel) {
    override fun str(): String = "#enter[$includeLevel] $filename\n"

    override fun cloneWith(pos: Position): AnyToken {
        return this
    }
}

class ExitIncludeGuard(filename: String, includeLevel: Int): PreprocessorGuard(filename, includeLevel) {
    override fun str(): String = "#exit[$includeLevel] $filename\n"

    override fun cloneWith(pos: Position): AnyToken {
        return this
    }
}

abstract class AnySpaceToken: AnyToken()

class Indent private constructor(private val spaces: Int): AnySpaceToken() {
    private val data by lazy { " ".repeat(spaces) }

    override fun str(): String = data

    override fun cloneWith(pos: Position): AnyToken {
        return of(spaces)
    }

    companion object {
        fun of(spaces: Int): Indent {
            assertion(spaces > 0) {
                "Indent should be greater than 0, but was $spaces"
            }

            return Indent(spaces)
        }
    }
}

class NewLine private constructor(private val spaces: Int): AnySpaceToken() {
    private val data by lazy { "\n".repeat(spaces) }

    override fun str(): String = data

    override fun cloneWith(pos: Position): AnyToken {
        return of(spaces)
    }

    companion object {
        fun of(lines: Int): NewLine {
            assertion(lines > 0) {
                "NewLine should be greater than 0, but was $lines"
            }
            return NewLine(lines)
        }
    }
}

abstract class CToken(private val position: Position): AnyToken() {
    fun line(): Int = position.line()
    fun pos(): Int = position.pos()

    fun position(): Position = position

    override fun toString(): String {
        return "'${str()}' in $position'"
    }
}

class Identifier(val data: String, position: Position): CToken(position) {
    override fun str(): String = data

    override fun cloneWith(pos: Position): CToken {
        return Identifier(data, pos)
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Identifier

        return data == other.data
    }

    companion object {
        val UNKNOWN = Identifier("<unknown>",  Position.UNKNOWN)
    }
}

class Punct(val data: Char, position: Position): CToken(position) {
    override fun str(): String = data.toString()

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Punct

        return data == other.data
    }

    override fun cloneWith(pos: Position): CToken {
        return Punct(data, pos)
    }
}

class Keyword(val data: String, position: Position): CToken(position) {
    init {
        assertion(LexicalElements.keywords.contains(data)) {
            "Keyword '$data' is not a keyword"
        }
    }

    override fun str(): String = data

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Keyword

        return data == other.data
    }

    override fun cloneWith(pos: Position): CToken {
        return Keyword(data, pos)
    }
}

class StringLiteral(val data: String, position: Position): CToken(position) {
    private val unquoted by lazy { data.substring(1, data.length - 1) }
    override fun str(): String = data

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as StringLiteral

        return data == other.data
    }

    fun unquote(): String {
        return unquoted
    }

    override fun cloneWith(pos: Position): CToken {
        return StringLiteral(data, pos)
    }

    companion object {
        fun quote(data: String, position: Position): StringLiteral {
            return StringLiteral("\"$data\"", position)
        }
    }
}

class Numeric(val data: Number, position: Position): CToken(position) {
    override fun str(): String = data.toString()

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Numeric

        return data == other.data
    }

    override fun cloneWith(pos: Position): CToken {
        return Numeric(data, pos)
    }
}