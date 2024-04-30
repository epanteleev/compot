package preprocess

import tokenizer.CToken
import tokenizer.OriginalPosition
import tokenizer.Position
import tokenizer.PreprocessedPosition


data class Macros(val name: String, private val value: List<CToken>) {
    fun cloneContentWith(macrosNamePos: Position): List<CToken> {
        val firstPos = value.first().position().pos()
        fun calculate(tok: CToken): CToken {
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