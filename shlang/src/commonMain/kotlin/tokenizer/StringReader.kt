package tokenizer

import kotlin.math.*


class StringReader(val str: String, var pos: Int = 0) {
    private val size get() = str.length
    val eof get() = pos >= size
    private val available get() = size - pos

    fun peek(): Char {
        return if (!eof) {
            str[pos]
        } else {
            '\u0000'
        }
    }

    fun check(char: Char): Boolean {
        return !eof && peek() == char
    }

    fun check(expect: String): Boolean {
        return !eof && str.startsWith(expect, pos)
    }

    fun inRange(offset: Int): Boolean = pos + offset < size

    fun peekOffset(offset: Int): Char {
        return if (pos + offset >= 0 && pos + offset < str.length) {
            str[pos + offset]
        } else {
            '\u0000'
        }
    }

    fun read(): Char {
        val p = pos++;
        return if (p >= 0 && p < str.length) {
            str[p]
        } else {
            '\u0000'
        }
    }

    fun readWhile(cond: (char: Char) -> Boolean): String {
        val start = pos
        while (!eof && cond(peek())) {
            read()
        }
        val end = pos
        return str.substring(start, end)
    }

    fun peek(count: Int): String {
        return str.substring(pos, pos + min(available, count))
    }

    fun read(count: Int) {
        pos += count
    }

    fun tryPeek(str: String): Boolean {
        for (n in str.indices) {
            if (peekOffset(n) != str[n]) {
                return false
            }
        }
        return true
    }

    inline fun readBlock(callback: () -> Unit): String {
        val startPos = pos
        callback()
        return str.substring(startPos, pos)
    }

    fun readCNumber(): String? {
        return tryRead {
            val start = pos
            if (peek() == '0' && (peekOffset(1) == 'x' || peekOffset(1) == 'X')) {
                read()
                read()
                while (!eof && (peek().isDigit() || peek().lowercaseChar() in 'a'..'f')) {
                    read()
                }
                val hexString = str.substring(start + 2, pos)
                return@tryRead hexString.toLong(16).toString() //TODO
            }

            read()
            while (!eof && peek().isDigit()) {
                read()
            }

            if (peek() == '.' && (peekOffset(1).isWhitespace() || peekOffset(1).isDigit())) {
                //Floating point value
                read()
                while (!eof && !isSeparator(peek())) {
                    read()
                }
                return@tryRead str.substring(start, pos)
            } else if (!peek().isLetter() && peek() != '_') {
                // Integer
                return@tryRead str.substring(start, pos)
            }

            if (check("ll") || check("LL")) {
                read(2)
                return@tryRead str.substring(start, pos)
            } else if (check("l") || check("L")) {
                read()
                return@tryRead str.substring(start, pos)
            }
            return null
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