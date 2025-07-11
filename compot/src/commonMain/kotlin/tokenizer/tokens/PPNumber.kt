package tokenizer.tokens

import tokenizer.Position
import types.CPrimitive


// 6.4.8 Preprocessing numbers
class PPNumber internal constructor(private val data: String, private var number: Number, val type: CPrimitive, position: Position):
    CToken(position) {
    constructor(number: Number, type: CPrimitive, pos: Position) : this(number.toString(), number, type, pos)

    override fun str(): String = data

    override fun hashCode(): Int {
        return data.hashCode()
    }

    fun number(): Number {
        return number
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PPNumber

        return data == other.data && number == other.number && type == other.type
    }

    override fun cloneWith(pos: Position): CToken {
        return PPNumber(data, number, type, pos)
    }

    override fun copy(): AnyToken {
        return PPNumber(data, number, type, position())
    }
}