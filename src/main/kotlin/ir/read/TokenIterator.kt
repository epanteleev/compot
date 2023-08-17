package ir.read

class TokenIterator(val iterator: Iterator<Token>): Iterator<Token> {

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    fun hasNextOrError(error: () -> Unit) {
        if (!iterator.hasNext()) {
            error()
        }
    }

    override fun next(): Token {
        return iterator.next()
    }

    fun nextOrError(message: String): Token {
        hasNextOrError { throw EOFException(message) }
        return iterator.next()
    }
}