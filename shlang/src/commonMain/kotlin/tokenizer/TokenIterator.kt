package tokenizer

interface TokenIterator: Iterator<AnyToken> {
    fun tokens(): MutableList<AnyToken>
}

class TokenIteratorImpl(private val tokens: MutableList<AnyToken>): TokenIterator {
    private var current: Int = 0

    override fun hasNext(): Boolean {
        return current < tokens.size
    }

    override fun next(): AnyToken {
        return tokens[current++]
    }

    override fun tokens(): MutableList<AnyToken> {
        return tokens
    }
}
