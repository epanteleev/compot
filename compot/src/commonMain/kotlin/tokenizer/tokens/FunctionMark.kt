package tokenizer.tokens

import tokenizer.Position

class FunctionMark: AnyStringLiteral(Position.UNKNOWN) {
    override fun str(): String = "__func__"

    override fun hashCode(): Int {
        return str().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        return other is FunctionMark
    }

    override fun cloneWith(pos: Position): CToken {
        return FunctionMark()
    }

    override fun copy(): AnyToken {
        return FunctionMark()
    }
}