package common

fun String.padTo(count: Int, pad: String): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append(this)
    for (i in 0 until count - length) {
        stringBuilder.append(pad)
    }
    return stringBuilder.toString()
}

fun String.quoted(): String {
    val stringBuilder = StringBuilder("\"")
    for (element in this) {
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