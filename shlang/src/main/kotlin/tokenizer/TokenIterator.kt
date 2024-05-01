package tokenizer

interface TokenIterator: Iterator<AnyToken> {
    fun toCTokenList(): MutableList<AnyToken>
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

    override fun toCTokenList(): MutableList<AnyToken> {
        val result = mutableListOf<AnyToken>()
        while (hasNext()) {
            val token = next()
            if (token is CToken) {
                result.add(token)
            }
        }
        return result
    }

    override fun tokens(): MutableList<AnyToken> {
        return tokens
    }
}
