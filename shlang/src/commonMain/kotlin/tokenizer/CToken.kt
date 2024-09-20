package tokenizer

import common.LListNode
import common.assertion


sealed class AnyToken: LListNode() {
    abstract fun str(): String

    abstract fun cloneWith(pos: Position): AnyToken

    override fun prev(): AnyToken? {
        return super.prev() as AnyToken?
    }

    override fun next(): AnyToken? {
        return super.next() as AnyToken?
    }
}

sealed class PreprocessorGuard(val filename: String, val includeLevel: Int, val line: Int): AnyToken()

class EnterIncludeGuard(filename: String, includeLevel: Int, line: Int): PreprocessorGuard(filename, includeLevel, line) {
    override fun str(): String = "#enter[$includeLevel] $filename in $line\n"

    override fun cloneWith(pos: Position): AnyToken {
        return this
    }
}

class ExitIncludeGuard(filename: String, includeLevel: Int, line: Int): PreprocessorGuard(filename, includeLevel, line) {
    override fun str(): String = "#exit[$includeLevel] $filename in $line\n"

    override fun cloneWith(pos: Position): AnyToken {
        return this
    }
}

sealed class AnySpaceToken: AnyToken()

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

sealed class CToken(private val position: Position): AnyToken() {
    fun line(): Int = position.line()
    fun pos(): Int = position.pos()

    fun position(): Position = position

    override fun toString(): String {
        return "'${str()}' in $position'"
    }
}

class Identifier(private val data: String, position: Position): CToken(position) {
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

class StringLiteral(private val data: String, position: Position): CToken(position) {
    override fun str(): String = "\"$data\""

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
        return data()
    }

    fun data(): String {
        val stringBuilder = StringBuilder()
        for (element in data) {
            if (element == '"') {
                stringBuilder.append("\\\"")
            } else {
                stringBuilder.append(element)
            }
        }
        return stringBuilder.toString()
    }

    override fun cloneWith(pos: Position): CToken {
        return StringLiteral(data, pos)
    }
}

class CharLiteral(val data: Char, position: Position): CToken(position) {
    override fun str(): String = "\'$data\'"

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CharLiteral

        return data == other.data
    }

    override fun cloneWith(pos: Position): CToken {
        return CharLiteral(data, pos)
    }
}

class Numeric(private val data: String, private val radix: Int, position: Position): CToken(position) {
    private var cachedNumber: Any? = null

    override fun str(): String = data

    override fun hashCode(): Int {
        return data.hashCode()
    }

    fun toNumberOrNull(): Any? {
        if (cachedNumber != null) {
            return cachedNumber
        }
        cachedNumber = when {
            data.endsWith("ULL") -> data.substring(0, data.length - 3).toULongOrNull(radix)
            data.endsWith("ull") -> data.substring(0, data.length - 3).toULongOrNull(radix)
            data.endsWith("LL")  -> data.substring(0, data.length - 2).toLongOrNull(radix)
            data.endsWith("ll")  -> data.substring(0, data.length - 2).toLongOrNull(radix)
            data.endsWith("LL")  -> data.substring(0, data.length - 2).toLongOrNull(radix)
            data.endsWith("ll")  -> data.substring(0, data.length - 2).toLongOrNull(radix)
            data.endsWith("UL")  -> data.substring(0, data.length - 2).toULongOrNull(radix)
            data.endsWith("ul")  -> data.substring(0, data.length - 2).toULongOrNull(radix)
            data.endsWith("L")   -> data.substring(0, data.length - 1).toLongOrNull(radix)
            data.endsWith("l")   -> data.substring(0, data.length - 1).toLongOrNull(radix)
            data.endsWith("U")   -> data.substring(0, data.length - 1).toULongOrNull(radix)
            data.endsWith("u")   -> data.substring(0, data.length - 1).toULongOrNull(radix)
            else -> data.toByteOrNull(radix) ?: data.toIntOrNull(radix) ?: data.toLongOrNull(radix) ?: data.toULongOrNull(radix) ?: data.toFloatOrNull() ?: data.toDoubleOrNull()
        }
        if (cachedNumber != null) {
            return cachedNumber
        }
        cachedNumber = when {
            data.endsWith("F") -> data.substring(0, data.length - 1).toFloatOrNull()
            data.endsWith("f") -> data.substring(0, data.length - 1).toFloatOrNull()
            data.endsWith("D") -> data.substring(0, data.length - 1).toDoubleOrNull()
            data.endsWith("d") -> data.substring(0, data.length - 1).toDoubleOrNull()
            else -> null
        }
        return cachedNumber
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Numeric

        return data == other.data
    }

    override fun cloneWith(pos: Position): CToken {
        return Numeric(data, radix, pos)
    }
}