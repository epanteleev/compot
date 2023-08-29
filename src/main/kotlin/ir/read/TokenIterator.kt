package ir.read

import kotlin.reflect.KClass

class TokenIterator(val tokenizer: Tokenizer): Iterator<Token> {

    override fun hasNext(): Boolean {
        tokenizer.skipWhitespace()
        return !tokenizer.isEnd()
    }

    fun hasNextOrError(error: () -> Unit) {
        if (!hasNext()) {
            error()
        }
    }

    override fun next(): Token {
        return tokenizer.nextToken()
    }

    fun nextOrError(message: String): Token {
        hasNextOrError { throw EOFException(message) }
        return next()
    }

    inline fun <reified T>expectOrError(errorMessage: String): T {
        val tok = nextOrError(errorMessage)

        if (tok !is T) {
            throw ParseErrorException(errorMessage, tok.message())
        }
        return tok
    }
}