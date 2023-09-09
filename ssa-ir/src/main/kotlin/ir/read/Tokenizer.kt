package ir.read

class EOFException(expect: String): Exception("Expect: $expect, but found EOF")

class Tokenizer(val data: String) {
    private var globalPosition = 0
    private var line = 0
    private var pos = 0

    private fun remainsLine(begin: Int): String {
        val end = data.indexOf('\n', begin)
        if (end == -1) {
            return data.substring(begin, data.length - begin)
        } else {
            return data.substring(begin, end)
        }
    }

    internal fun isEnd(): Boolean {
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

    private fun skipComments() {
        nextChar()
        var ch = getChar()
        while (!isEnd() && ch != '\n') {
            nextChar()
            ch = getChar()
        }
        nextLine()
    }

    internal fun skipWhitespace() {
        if (isEnd()) {
            return
        }

        var ch = getChar()
        while (!isEnd() && ch.isWhitespace() || ch == ';') {
            when (ch) {
                ';' -> skipComments()
                '\n' -> nextLine()
                else -> nextChar()
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

    private fun readIdentifierOrKeywordOrType(): Token {
        val string = readString()
        if (string == "define") {
            return Define(line, pos)
        } else if (string == "extern") {
            return Extern(line, pos)
        } else if (string == "void") {
            return TypeToken("void", 0, line, pos)
        }

        val begin = pos - string.length
        return if (!isEnd() && getChar() == ':') {
            nextChar()
            LabelToken(string, line, begin)
        } else {
            Identifier(string, line, begin)
        }
    }

    private fun readTypeOrIdentifier(): Token {
        val begin = globalPosition
        nextChar()
        if (!getChar().isDigit()) {
            previousChar()
            return readIdentifierOrKeywordOrType()
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
        val pos = pos
        nextChar()
        while (!isEnd() && getChar().isDigit()) {
            nextChar()
        }

        if (getChar() == '.') {
            //Floating point value
            nextChar()
            while (!isEnd() && getChar().isDigit()) {
                nextChar()
            }

            if (!isEnd() && !getChar().isWhitespace()) {
                throw RuntimeException("$line:$pos cannot parse floating point value: ${remainsLine(begin)}")
            }
            val floatString = data.substring(begin, globalPosition)
            val float = floatString.toDoubleOrNull()
                ?: throw RuntimeException("Cannot to be converted to floating point value: $floatString")
            return FloatValue(float, line, pos)

        } else if (getChar().isLetter()) {
            // Identifier
            while (!isEnd() && !getChar().isWhitespace()) {
                nextChar()
            }

            val string = data.substring(begin, globalPosition)
            return Identifier(string, line, pos)

        } else {
            // Integer
            val intString = data.substring(begin, globalPosition)
            val intOrNull = intString.toLongOrNull() ?:
                throw RuntimeException("$line:$pos cannot to be converted to integer: $intString")
            return IntValue(intOrNull, line, pos)
        }
    }

    internal fun nextToken(): Token {
        skipWhitespace()
        val ch = getChar()
        val tok = when (ch) {
            '{' -> OpenBrace(line, pos)
            '}' -> CloseBrace(line, pos)
            '(' -> OpenParen(line, pos)
            ')' -> CloseParen(line, pos)
            '=' -> Equal(line, pos)
            ',' -> Comma(line, pos)
            ':' -> Colon(line, pos)
            '.' -> Dot(line, pos)
            '[' -> OpenSquareBracket(line, pos)
            ']' -> CloseSquareBracket(line, pos)
            else -> null
        }
        if (tok != null) {
            nextChar()
            return tok
        }

        return if (ch.isDigit()) {
            readNumberOrIdentifier()
        } else if (ch == 'u' || ch == 'i' || ch == 'f') {
            readTypeOrIdentifier()
        } else if (ch.isLetter()) {
            readIdentifierOrKeywordOrType()
        } else if (ch == '%') {
            nextChar()
            val name = readString()
            val start = pos - 1 - name.length
            ValueToken(name, line, start)
        } else {
            throw RuntimeException("$line:$pos cannot parse: ${remainsLine(globalPosition)}")
        }
    }

    operator fun iterator(): TokenIterator {
        return TokenIterator(this)
    }
}