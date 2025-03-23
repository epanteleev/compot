package tokenizer.tokens

import common.quotedEscapes
import preprocess.Hideset
import tokenizer.Position


class StringLiteral(private val data: String, position: Position): CToken(position, Hideset()) {
    override fun str(): String = data.quotedEscapes()

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as StringLiteral

        return data == other.data
    }

    fun data(): String = data

    fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    override fun cloneWith(pos: Position): CToken {
        return StringLiteral(data, pos)
    }
}