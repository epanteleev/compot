package preprocess

import common.assertion
import tokenizer.*
import common.forEachWith
import preprocess.Macros.Companion.newTokenFrom
import kotlin.jvm.JvmStatic


data class MacroExpansionException(override val message: String): Exception(message)

abstract class Macros(val name: String) {
    abstract fun first(): CToken

    abstract fun tokenString(): String

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Macros

        return name == other.name
    }

    companion object {
        fun newTokenFrom(macrosNamePos: Position, tok: AnyToken): AnyToken {
            if (tok !is CToken) {
                return tok.cloneWith(macrosNamePos)
            }

            val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, tok.position() as OriginalPosition)
            return tok.cloneWith(preprocessedPosition)
        }
    }
}

class MacroDefinition(name: String): Macros(name) {
    override fun first(): CToken {
        throw MacroExpansionException("Macro definition cannot be expanded")
    }

    override fun tokenString(): String {
        return "#define $name"
    }
}

class PredefinedMacros(name: String, private val callback: (Position) -> TokenList): Macros(name) {
    override fun first(): CToken {
        return callback(Position.UNKNOWN).first() as CToken
    }

    override fun tokenString(): String {
        return "#define $name ${callback(Position.UNKNOWN).joinToString("") { it.str() }}"
    }

    fun cloneContentWith(macrosNamePos: Position): TokenList {
        val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, OriginalPosition.UNKNOWN)

        return callback(preprocessedPosition)
    }

    fun constEval(): Long {
        val tokens = callback(Position.UNKNOWN)
        val token = tokens.first()
        assertion(tokens.size == 1) { "invariant"}

        if (token !is Numeric) {
            throw PreprocessorException("Predefined macro '$name' is not a number")
        }

        return token.toNumberOrNull() as Long
    }
}

class MacroReplacement(name: String, val value: TokenList): Macros(name) {
    override fun first(): CToken {
        return value.first() as CToken
    }

    override fun tokenString(): String {
        return "#define $name ${value.joinToString("") { it.str() }}"
    }

    fun substitute(macrosNamePos: Position): TokenList {
        val result = TokenList()
        for (tok in value) {
            result.add(newTokenFrom(macrosNamePos, tok))
        }

        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MacroReplacement

        if (name != other.name) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}

class MacroFunction(name: String, internal val argNames: CTokenList, internal val value: TokenList): Macros(name) {
    override fun first(): CToken {
        return value.first() as CToken
    }

    override fun tokenString(): String {
        return "#define $name(${argNames.joinToString(", ") { it.str() }}) ${value.joinToString(" ") { it.str() }}"
    }
}