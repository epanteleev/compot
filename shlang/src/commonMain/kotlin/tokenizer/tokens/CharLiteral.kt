package tokenizer.tokens

import tokenizer.Position


class CharLiteral(private val data: Char, position: Position): CToken(position) {
    override fun str(): String = "\'$data\'"

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CharLiteral

        return data == other.data
    }

    fun code(): Byte {
        return data.code.toByte()
    }

    override fun cloneWith(pos: Position): CToken {
        return CharLiteral(data, pos)
    }

    override fun copy(): CToken {
        return CharLiteral(data, position())
    }
}