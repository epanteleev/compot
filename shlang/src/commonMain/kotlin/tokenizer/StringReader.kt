package tokenizer

import kotlin.math.*


class StringReader(val str: String, var pos: Int = 0) {
    private val size
        get() = str.length
    val eof
        get() = pos >= size
    private val available
        get() = size - pos

    fun eof(offset: Int): Boolean {
        return pos + offset >= size
    }

    fun peek(): Char {
        if (eof) {
            throw IllegalStateException("EOF")
        }
        return str[pos]
    }

    inline fun isSpace(): Boolean {
        return peek() == ' ' || peek() == '\t' || peek() == '\r'
    }

    fun check(char: Char): Boolean {
        return !eof && str[pos] == char
    }

    fun check(expect: String): Boolean {
        return str.startsWith(expect, pos)
    }

    fun inRange(offset: Int): Boolean = pos + offset < size

    fun peekOffset(offset: Int): Char {
        return if (pos + offset >= 0 && pos + offset < str.length) {
            str[pos + offset]
        } else {
            throw IllegalStateException("EOF")
        }
    }

    fun read(): Char {
        val p = pos++;
        return if (p >= 0 && p < str.length) {
            str[p]
        } else {
            throw IllegalStateException("EOF")
        }
    }

    fun peek(count: Int): String {
        return str.substring(pos, pos + min(available, count))
    }

    fun read(count: Int) {
        pos += count
    }

    inline fun readCharLiteral(): Char {
        if (peek() == '\\') {
            read()
            val ch = when (peek()) {
                'n' -> '\n'
                't' -> '\t'
                'r' -> '\r'
                '0' -> '\u0000'
                '\'' -> '\''
                '\\' -> '\\'
                else -> throw IllegalStateException("Unknown escape character")
            }
            read()
            if (peek() != '\'') {
                throw IllegalStateException("Expected closing quote")
            }
            read()
            return ch
        } else {
            val ch = read()
            if (peek() != '\'') {
                throw IllegalStateException("Expected closing quote")
            }
            read()
            return ch
        }
    }

    inline fun readIdentifier(): String = readBlock {
        while (!eof && (peek().isLetter() || peek().isDigit() || check('_'))) {
            read()
        }
    }

    inline fun readBlock(callback: () -> Unit): String {
        val startPos = pos
        callback()
        return str.substring(startPos, pos)
    }

    private fun tryGetSuffix(start: Int): String? {
        if (check("ll") || check("LL")) {
            read(2)
            return str.substring(start, pos)
        } else if (check("LLU") || check("llu")) {
            read(3)
            return str.substring(start, pos)
        } else if (check("ULL") || check("ull")) {
            read(3)
            return str.substring(start, pos)
        } else if (check("LU") || check("lu")) {
            read(2)
            return str.substring(start, pos)
        } else if (check("UL") || check("ul")) {
            read(2)
            return str.substring(start, pos)
        } else if (check("U") || check("u")) {
            read()
            return str.substring(start, pos)
        } else if (check("L") || check("l")) {
            read()
            return str.substring(start, pos)
        } else if (check("F") || check("f")) {
            read()
            return str.substring(start, pos)
        } else if (check("D") || check("d")) {
            read()
            return str.substring(start, pos)
        }
        return null
    }

    fun readCNumber(): Pair<String, Int>? {
        return tryRead {
            val start = pos
            if (peek() == '0' && (peekOffset(1) == 'x' || peekOffset(1) == 'X')) {
                read()
                read()
                while (!eof && (peek().isDigit() || peek().lowercaseChar() in 'a'..'f')) {
                    read()
                }
                val withPostfix = tryGetSuffix(start + 2)
                if (withPostfix != null) {
                    return@tryRead Pair(withPostfix, 16)
                }
                val hexString = str.substring(start + 2, pos)
                return@tryRead Pair(hexString, 16)
            }

            read()
            while (!eof && (peek().isDigit() || peek() == 'e' || peek() == 'E' || peek() == 'p' || peek() == 'P')) {
                read()
            }
            if (eof) {
                return@tryRead Pair(str.substring(start, pos), 10)
            }

            if (check('.') && (eof(1) ||
                        peekOffset(1).isWhitespace() ||
                        peekOffset(1).isDigit() ||
                        peekOffset(1) == 'f' ||
                        peekOffset(1) == 'F')) {
                //Floating point value
                read()
                while (!eof && !isSeparator(peek())) {
                    read()
                }
                val result = tryGetSuffix(start) ?: str.substring(start, pos)
                return@tryRead Pair(result, 10)
            } else if (!peek().isLetter() && peek() != '_') {
                // Integer
                val result = tryGetSuffix(start) ?: str.substring(start, pos)
                return@tryRead Pair(result, 10)
            }

            val postfix = tryGetSuffix(start)
            if (postfix != null) {
                return@tryRead Pair(postfix, 10)
            }
            return@tryRead null
        }
    }

    private inline fun <T> tryRead(callback: () -> T?): T? {
        val old = pos
        val result = callback()
        if (result == null) {
            pos = old
        }
        return result
    }

    companion object {
        private val punctuation = arrayOf(
            '!', '"', '#', '$', '%', '&', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=',
            '>','?','@','[','\\',']','^','`','{','|','}','~',' ','\t','\n'
        )

        fun tryPunct(ch: Char): Boolean {
            return punctuation.contains(ch)
        }

        fun isSeparator(char: Char): Boolean {
            return punctuation.contains(char)
        }
    }
}