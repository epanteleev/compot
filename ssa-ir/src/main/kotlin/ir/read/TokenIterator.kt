package ir.read

class TokenIterator(val tokenizer: Tokenizer) : Iterator<Token> {
    fun hasNextOrError(error: () -> Unit) {
        if (!hasNext()) {
            error()
        }
    }

    override fun hasNext(): Boolean {
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

    inline fun <reified T: Token> expect(errorMessage: String): T {
        val tok = next(errorMessage)

        if (tok !is T) {
            throw ParseErrorException(errorMessage, tok)
        }
        return tok
    }
}