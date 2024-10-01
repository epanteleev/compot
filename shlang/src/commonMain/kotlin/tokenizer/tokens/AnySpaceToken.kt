package tokenizer.tokens

import common.assertion
import tokenizer.Position

sealed class AnySpaceToken: AnyToken()

class Indent private constructor(private val spaces: Int): AnySpaceToken() {
    private val data by lazy { " ".repeat(spaces) }

    override fun str(): String = data

    override fun cloneWith(pos: Position): AnyToken {
        return of(spaces)
    }

    companion object {
        fun of(spaces: Int): Indent {
            assertion(spaces > 0) {
                "Indent should be greater than 0, but was $spaces"
            }

            return Indent(spaces)
        }
    }
}

class NewLine private constructor(private val spaces: Int): AnySpaceToken() {
    private val data by lazy { "\n".repeat(spaces) }

    override fun str(): String = data

    override fun cloneWith(pos: Position): AnyToken {
        return of(spaces)
    }

    companion object {
        fun of(lines: Int): NewLine {
            assertion(lines > 0) {
                "NewLine should be greater than 0, but was $lines"
            }
            return NewLine(lines)
        }
    }
}