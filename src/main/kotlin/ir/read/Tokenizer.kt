package ir.read

class EOFException(expect: String): Exception("Expect: $expect, but found EOF")

class Tokenizer(val data: String) {
    private var globalPosition = 0
    private var line = 0
    private var pos = 0
    private val tokens = arrayListOf<Token>()

    init {
        while (!isEnd()) {
            tokens.add(nextToken())
        }
    }

    private fun isEnd(): Boolean {
        return globalPosition >= data.length
    }

    private fun nextChar() {
        globalPosition += 1
        pos += 1
    }

    private fun previousChar() {
        globalPosition -= 1
        pos -= 1
    }

    private fun nextLine() {
        line += 1
        pos = 0
        globalPosition += 1
    }

    private fun getChar(): Char {
        return data[globalPosition]
    }

    private fun skipWhitespace() {
        var ch = getChar()
        while (ch.isWhitespace()) {
            if (ch == '\n') {
                nextLine()
            } else {
                nextChar()
            }

            if (isEnd()) {
                return
            }
            ch = getChar()
        }
    }

    private fun readString(): String {
        val begin = globalPosition
        nextChar()
        while (!isEnd() && getChar().isLetterOrDigit()) {
            nextChar()
        }
        val end = globalPosition
        return data.substring(begin, end)
    }

    private fun readIdentifierOrKeyword(): Token {
        val string = readString()
        if (string == "define") {
            return Define(line, pos)
        } else if (string == "extern") {
            return Extern(line, pos)
        }

        return Identifier(string, line, pos - string.length)
    }

    private fun readTypeOrIdentifier(): Token {
        val begin = globalPosition
        nextChar()
        if (!getChar().isDigit()) {
            previousChar()
            return readIdentifierOrKeyword()
        }

        while (!isEnd() && getChar().isDigit()) {
            nextChar()
        }

        val typeName = data.substring(begin, globalPosition)
        val pointersBegin = globalPosition
        while (!isEnd() && getChar() == '*') {
            nextChar()
        }
        val indirection = if (pointersBegin != globalPosition) {
            globalPosition - pointersBegin
        } else {
            0
        }

        val start = pos - indirection - typeName.length
        return TypeToken(typeName, indirection, line, start)
    }

    private fun readNumberOrIdentifier(): Token {
        val begin = globalPosition
        nextChar()
        while (!isEnd() && getChar().isDigit()) {
            nextChar()
        }

        if (getChar() == '.') {
            nextChar()
            while (!isEnd() && getChar().isDigit()) {
                nextChar()
            }

            val floatString = data.substring(begin, globalPosition)
            val float = floatString.toDoubleOrNull()
                ?: throw RuntimeException("Cannot to be converted to floating point value: $floatString")
            return FloatValue(float, line, pos - floatString.length)

        } else if (getChar().isLetter()) {
            while (!isEnd() && !getChar().isWhitespace()) {
                nextChar()
            }

            val end = globalPosition
            val string = data.substring(begin, end)
            return Identifier(string, line, pos - string.length)

        } else {
            val intString = data.substring(begin, globalPosition)
            val intOrNull = intString.toLongOrNull() ?: throw RuntimeException("Cannot to be converted to integer: $intString")
            return IntValue(intOrNull, line, pos - intString.length)
        }
    }
    private fun nextToken(): Token {
        skipWhitespace()
        val ch = getChar()
        val tok = when (ch) {
            '{' -> OpenBrace(line, pos)
            '}' -> CloseBrace(line, pos)
            '(' -> OpenParen(line, pos)
            ')' -> CloseParen(line, pos)
            '=' -> Equal(line, pos)
            ',' -> Comma(line, pos)
            ';' -> Colon(line, pos)
            else -> null
        }
        if (tok != null) {
            nextChar()
            return tok
        }

        if (ch.isDigit()) {
            return readNumberOrIdentifier()
        } else if (ch == 'u' || ch == 'i' || ch == 'f') {
            return readTypeOrIdentifier()
        } else if (ch.isLetter()) {
            return readIdentifierOrKeyword()
        } else if (ch == '%') {
            nextChar()
            val name = readString()
            val start = pos - 1 - name.length
            return ValueToken(name, line, start)
        } else {
            throw RuntimeException("Cannot parse: ${data.substring(globalPosition)}")
        }
    }

    operator fun iterator(): TokenIterator {
        return TokenIterator(tokens.iterator())
    }
}