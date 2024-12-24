package tokenizer.tokens

import tokenizer.Position

sealed class CToken(private val position: Position): AnyToken() {
    fun line(): Int = position.line()
    fun pos(): Int = position.pos()

    fun position(): Position = position

    override fun toString(): String {
        return "'${str()}' in $position'"
    }
}

inline fun<reified T: CToken> CToken.asToken(): T {
    if (this !is T) {
        throw IllegalArgumentException("Expected ${T::class.simpleName}, but got ${this::class.simpleName}")
    }

    return this
}