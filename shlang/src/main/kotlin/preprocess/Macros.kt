package preprocess

import tokenizer.*


data class Macros(val name: String, private val value: List<AnyToken>) {
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