package ir.read

import ir.read.bulder.ParseErrorException

class TokenIterator(val tokenizer: Tokenizer) : Iterator<Token> {
    fun hasNextOrError(error: () -> Unit) {
        if (!hasNext()) {
            error()
        }
    }

    override fun hasNext(): Boolean {
        if (tokenizer.isEnd()) {
            return false
        }
        tokenizer.skipWhitespace()
        return !tokenizer.isEnd()
    }

    override fun next(): Token {
        return tokenizer.nextToken()
    }

    fun next(message: String): Token {
        hasNextOrError { throw EOFException(message) }
        return next()
    }

    inline fun <reified T: Token> expect(expect: String): T {
        val tok = next(expect)

        if (tok !is T) {
            throw ParseErrorException(expect, tok)
        }
        return tok
    }
}