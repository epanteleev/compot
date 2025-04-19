package tokenizer.tokens

import common.assertion
import preprocess.macros.Macros
import tokenizer.Position

sealed class AnySpaceToken: AnyToken()

class Indent private constructor(private val spaces: Int): AnySpaceToken() {
    private val data by lazy { " ".repeat(spaces) }

    override fun str(): String = data

    override fun copy(): AnyToken {
        return of(spaces)
    }

    override fun hashCode(): Int {
        return Indent::class.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Indent

        return spaces == other.spaces
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

    override fun copy(): AnyToken {
        return of(spaces)
    }

    override fun hashCode(): Int {
        return NewLine::class.hashCode() xor spaces
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NewLine

        return spaces == other.spaces
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