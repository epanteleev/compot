package tokenizer


class TokenPrinter private constructor(val tokens: List<CToken>) {
    private val stringBuilder = StringBuilder()

    private fun print(previous: CToken, current: CToken) {
        if (CToken.hasNewLine(previous, current)) {
            stringBuilder.append("\n")
        }

        val spaces = CToken.countSpaces(previous, current)
        if (spaces > 0) {
            stringBuilder.append(" ".repeat(spaces))
        }

        stringBuilder.append(current.str())
    }

    private fun construct(): String {
        var previous = tokens.first()
        stringBuilder.append("\n".repeat(previous.line() - 1))

        stringBuilder.append(" ".repeat(previous.pos()))
        stringBuilder.append(previous.str())
        for (i in 1 until tokens.size) {
            val current = tokens[i]
            print(previous, current)
            previous = current
        }
        return stringBuilder.toString()
    }

    companion object {
        fun print(tokens: List<CToken>): String {
            return TokenPrinter(tokens).construct()
        }
    }
}