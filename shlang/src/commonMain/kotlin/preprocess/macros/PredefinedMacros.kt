package preprocess.macros

import tokenizer.*
import common.assertion
import tokenizer.tokens.*
import preprocess.PreprocessorException


class PredefinedMacros(name: String, private val callback: (Position) -> TokenList): Macros(name) {
    override fun first(): CToken {
        return callback(Position.UNKNOWN).first() as CToken
    }

    override fun tokenString(): String {
        val builder = StringBuilder("#define ")
        builder.append(name)
            .append(' ')

        val result = callback(Position.UNKNOWN)
        result.joinTo(builder) { it.str() }

        return builder.toString()
    }

    fun cloneContentWith(macrosNamePos: Position): TokenList {
        val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, OriginalPosition.UNKNOWN)
        return callback(preprocessedPosition)
    }

    fun constEval(): Long {
        val tokens = callback(Position.UNKNOWN)
        val token = tokens.first()
        assertion(tokens.size == 1) { "invariant"}

        if (token !is PPNumber) {
            throw PreprocessorException("Predefined macro '$name' is not a number")
        }

        return token.toNumberOrNull() as Long
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as PredefinedMacros

        return callback == other.callback
    }


}