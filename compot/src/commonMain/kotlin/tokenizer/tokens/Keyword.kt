package tokenizer.tokens

import common.assertion
import preprocess.Hideset
import tokenizer.LexicalElements
import tokenizer.Position


class Keyword(val data: String, position: Position, private val hideset: Hideset): CToken(position), MacrosName {
    init {
        assertion(LexicalElements.keywords.contains(data)) {
            "Keyword '$data' is not a keyword"
        }
    }

    override fun str(): String = data
    override fun hideset(): Hideset = hideset

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Keyword

        return data == other.data
    }

    override fun cloneWith(pos: Position): CToken {
        return Keyword(data, pos, hideset.copy())
    }

    override fun copy(): AnyToken {
        return Keyword(data, position(), hideset.copy())
    }
}