package preprocess

import tokenizer.*

abstract class AnyMacros(val name: String) {
    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnyMacros

        return name == other.name
    }
}

class MacroDefinition(name: String): AnyMacros(name)

class MacroReplacement(name: String, private val value: List<AnyToken>): AnyMacros(name) {
    private fun first(): CToken {
        return value.first() as CToken
    }

    private fun last(): CToken {
        return value.last() as CToken
    }

    fun cloneContentWith(macrosNamePos: Position): List<AnyToken> {
        val firstPos = first().position().pos()
        fun calculate(tok: AnyToken): AnyToken {
            if (tok !is CToken) {
                return tok
            }
            val realPos = tok.position().pos() - firstPos
            val preprocessedPosition = PreprocessedPosition(macrosNamePos.line(),
                macrosNamePos.pos() + realPos,
                macrosNamePos.filename(),
                tok.position() as OriginalPosition
            )

            return tok.cloneWith(preprocessedPosition)
        }

        return value.map { calculate(it) }
    }
}