package tokenizer.tokens

import common.assertion
import tokenizer.LexicalElements
import tokenizer.Position


class Punctuator(val data: String, position: Position): CToken(position) {
    init {
        assertion(LexicalElements.allPunctuators.contains(data)) {
            "Operator '$data' is not an operator"
        }
    }

    override fun str(): String = data

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Punctuator

        return data == other.data
    }

    override fun cloneWith(pos: Position): CToken {
        return Punctuator(data, pos)
    }

    override fun copy(): AnyToken {
        return Punctuator(data, position())
    }
}