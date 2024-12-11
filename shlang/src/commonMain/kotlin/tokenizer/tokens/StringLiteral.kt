package tokenizer.tokens

import tokenizer.Position


class StringLiteral(private val data: String, position: Position): CToken(position) {
    override fun str(): String = quote()

    override fun hashCode(): Int {
        return data.hashCode()
    }

    private fun quote(): String {
        val stringBuilder = StringBuilder("\"")
        for (element in data) {
            val ch = when (element) {
                '\n'     -> "\\n"
                '\t'     -> "\\t"
                '\r'     -> "\\r"
                '\b'     -> "\\b"
                '\u000C' -> "\\f"
                '\u0007' -> "\\a"
                '\u0000' -> "\\0"
                '\\'     -> "\\\\"
                '"'      -> "\\\""
                else     -> element
            }
            stringBuilder.append(ch)
        }
        stringBuilder.append("\"")
        return stringBuilder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as StringLiteral

        return data == other.data
    }

    fun unquote(): String {
        return data()
    }

    fun data(): String {
        val stringBuilder = StringBuilder()
        for (element in data) {
            if (element == '"') {
                stringBuilder.append("\\\"")
            } else {
                stringBuilder.append(element)
            }
        }
        return stringBuilder.toString()
    }

    override fun cloneWith(pos: Position): CToken {
        return StringLiteral(data, pos)
    }
}