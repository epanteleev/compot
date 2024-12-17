package tokenizer.tokens

import tokenizer.Position

// 6.4.8 Preprocessing numbers
class PPNumber internal constructor(private val data: String, private var number: Number, position: Position): CToken(position) {
    constructor(number: Number, pos: Position) : this(number.toString(), number, pos)

    override fun str(): String = data

    override fun hashCode(): Int {
        return data.hashCode()
    }

    fun toNumberOrNull(): Number {
        return number
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PPNumber

        return data == other.data
    }

    override fun cloneWith(pos: Position): CToken {
        return PPNumber(data, number, pos)
    }
}