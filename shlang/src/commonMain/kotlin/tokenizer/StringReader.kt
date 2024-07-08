package tokenizer

import kotlin.math.*


class StringReader(val str: String, var pos: Int = 0) {
    private val size get() = str.length
    val eof get() = pos >= size
    private val available get() = size - pos

    fun peek(): Char {
        return if (pos >= 0 && pos < str.length){
            str[pos]
        } else {
            '\u0000'
        }
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
        peek(count).also { pos += it.length }
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

    fun readNumeric(): String? {
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

            val suffix = peek()
            if (suffix == 'L' || suffix == 'l') {
                read()
                val suffix1 = peek()
                if (suffix1 == 'L' || suffix1 == 'l') {
                    read()
                    return@tryRead str.substring(start, pos - 2)
                } else {
                    return@tryRead str.substring(start, pos - 1)
                }
            } else if (peek() == 'U' || peek() == 'u') {
                read()
                if (peek() == 'U' || peek() == 'u') {
                    read()
                    return@tryRead str.substring(start, pos - 2)
                } else {
                    return@tryRead str.substring(start, pos - 1)
                }
            } else {
                return@tryRead null
            }
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