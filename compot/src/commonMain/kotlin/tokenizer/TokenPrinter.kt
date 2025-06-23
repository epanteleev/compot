package tokenizer

import tokenizer.tokens.ExitIncludeGuard


object TokenPrinter {
    fun print(tokens: TokenList): String {
        val stringBuilder = StringBuilder()
        for (token in tokens) {
            if (token is ExitIncludeGuard) {
                stringBuilder.append('\n')
            }
            stringBuilder.append(token.str())
        }
        return stringBuilder.toString()
    }
}