package common

import kotlin.text.iterator

private fun wrapEscapedChar(char: Char): String? = when (char) {
    '\n'     -> "\\n"
    '\t'     -> "\\t"
    '\r'     -> "\\r"
    '\b'     -> "\\b"
    '\u000C' -> "\\f"
    '\u0007' -> "\\x07"
    '\u0000' -> "\\0"
    '\\'     -> "\\\\"
    '"'      -> "\\\""
    else     -> null
}

fun String.quotedEscapes(): String {
    val stringBuilder = StringBuilder("\"")
    wrapEscapes(stringBuilder)
    stringBuilder.append("\"")
    return stringBuilder.toString()
}

fun String.asCString(): String {
    val sb = StringBuilder("\"")
    for (element in this) {
        val code = element.code
        if (code in 128..255) {
            cvtToOctalSequence(code, sb)
            continue
        }

        val ch = wrapEscapedChar(element) ?: element
        sb.append(ch)
    }

    sb.append("\"")
    return sb.toString()
}

private fun cvtToOctalSequence(code: Int, cb: StringBuilder) {
    val octal = code.toString(8)
    when (octal.length) {
        3 -> cb.append("\\$octal")
        4 -> cb.append("\\3${octal[0]}${octal[1]}$\\2${octal[2]}${octal[3]}")
        6 -> cb.append("\\3${octal[0]}${octal[1]}$\\2${octal[2]}${octal[3]}$\\2${octal[4]}${octal[5]}")
        else -> throw IllegalArgumentException("Invalid octal value: $octal")
    }
}

private fun String.wrapEscapes(stringBuilder: StringBuilder) {
    for (element in this) {
        val ch = wrapEscapedChar(element) ?: element
        stringBuilder.append(ch)
    }
}