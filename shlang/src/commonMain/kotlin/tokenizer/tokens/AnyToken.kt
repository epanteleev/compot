package tokenizer.tokens

import common.LListNode
import tokenizer.Position

sealed class AnyToken: LListNode() {
    abstract fun str(): String

    abstract fun cloneWith(pos: Position): AnyToken

    override fun prev(): AnyToken? {
        return super.prev() as AnyToken?
    }

    override fun next(): AnyToken? {
        return super.next() as AnyToken?
    }

    abstract override fun hashCode(): Int

    abstract override fun equals(other: Any?): Boolean
}

inline fun<reified T: AnyToken> AnyToken.asToken(): T {
    if (this !is T) {
        throw IllegalArgumentException("Expected ${T::class.simpleName}, but got ${this::class.simpleName}")
    }

    return this
}