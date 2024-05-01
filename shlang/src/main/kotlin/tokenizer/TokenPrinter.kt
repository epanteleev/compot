package tokenizer


class TokenPrinter private constructor(val tokens: List<AnyToken>) {
    private val stringBuilder = StringBuilder()


    private fun construct(): String {
        for (token in tokens) {
            stringBuilder.append(token.str())
        }
        return stringBuilder.toString()
    }

    companion object {
        fun print(tokens: List<AnyToken>): String {
            return TokenPrinter(tokens).construct()
        }
    }
}