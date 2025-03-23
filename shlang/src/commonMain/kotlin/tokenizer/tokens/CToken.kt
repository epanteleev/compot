package tokenizer.tokens

import preprocess.Hideset
import tokenizer.Position

sealed class CToken(private val position: Position, val hideset: Hideset): AnyToken() {
    fun line(): Int = position.line()
    fun pos(): Int = position.pos()

    fun position(): Position = position

    override fun toString(): String {
        return "'${str()}' in $position'"
    }
}