package tokenizer

interface TokenIterator: Iterator<AnyToken> {
    fun toCTokenList(): List<CToken>
    fun tokens(): List<AnyToken>
}

class TokenIteratorImpl(private val tokens: List<AnyToken>): TokenIterator {
    private var current: Int = 0

    override fun hasNext(): Boolean {
        return current < tokens.size
    }

    override fun next(): AnyToken {
        return tokens[current++]
    }

    override fun toCTokenList(): List<CToken> {
        val result = mutableListOf<CToken>()
        while (hasNext()) {
            val token = next()
            if (token is CToken) {
                result.add(token)
            }
        }
        return result
    }

    override fun tokens(): List<AnyToken> {
        return tokens
    }
}
