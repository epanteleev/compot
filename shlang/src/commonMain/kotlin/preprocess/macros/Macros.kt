package preprocess.macros

import tokenizer.*
import tokenizer.tokens.*


data class MacroExpansionException(override val message: String): Exception(message)

sealed class Macros(val name: String) {
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