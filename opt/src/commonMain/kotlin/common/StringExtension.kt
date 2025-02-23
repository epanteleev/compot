package common

fun String.padTo(count: Int, pad: String): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append(this)
    for (i in 0 until count - length) {
        stringBuilder.append(pad)
    }
    return stringBuilder.toString()
}

private fun wrapEscapedChar(char: Char): String? = when (char) {
    '\n'     -> "\\n"
    '\t'     -> "\\t"
    '\r'     -> "\\r"
    '\b'     -> "\\b"
    '\u000C' -> "\\f"
    '\u0007' -> "\\a"
    '\u0000' -> "\\0"
    '\\'     -> "\\\\"
    else     -> null
}

fun String.quotedEscapes(): String {
    val stringBuilder = StringBuilder("\"")
    for (element in this) {
        val ch = wrapEscapedChar(element) ?: element
        stringBuilder.append(ch)
    }
    stringBuilder.append("\"")
    return stringBuilder.toString()
}

fun String.unquoted(): String {
    val stringBuilder = StringBuilder()
    var i = 0
    while (i < length) {
        val ch = this[i]
        if (ch == '\\') {
            i++
            val unescaped = when (val escaped = this[i]) {
                'n'  -> '\n'
                't'  -> '\t'
                'r'  -> '\r'
                'b'  -> '\b'
                'f'  -> '\u000C'
                'a'  -> '\u0007'
                '0'  -> '\u0000'
                '\\' -> '\\'
                '"'  -> '"'
                else -> throw IllegalArgumentException("Unknown escape character: $escaped")
            }
            stringBuilder.append(unescaped)
        } else {
            stringBuilder.append(ch)
        }
        i++
    }
    return stringBuilder.toString()
}