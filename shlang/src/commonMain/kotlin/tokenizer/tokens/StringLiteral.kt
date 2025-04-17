package tokenizer.tokens

import common.quotedEscapes
import tokenizer.Position

class StringLiteral(private val data: String, position: Position): AnyStringLiteral(position) {
    override fun str(): String = data.quotedEscapes()

    override fun hashCode(): Int {
        return data.hashCode()
    }

    fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    fun data(): String = data

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as StringLiteral

        return data == other.data
    }

    override fun cloneWith(pos: Position): CToken {
        return StringLiteral(data, pos)
    }
}