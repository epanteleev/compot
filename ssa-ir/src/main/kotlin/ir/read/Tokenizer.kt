package ir.read

class EOFException(expect: String): Exception("Expect: $expect, but found EOF")

class Tokenizer(val data: String) {
    private var globalPosition = 0
    private var line = 1
    private var pos = 0

    private fun remainsLine(begin: Int): String {
        val end = data.indexOf('\n', begin)
        return if (end == -1) {
            data.substring(begin, data.length - begin)
        } else {
            data.substring(begin, end)
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

    internal fun skipWhitespace() {
        fun skipComments() {
            nextChar()
            var ch = getChar()
            while (!isEnd() && ch != '\n') {
                nextChar()
                ch = getChar()
            }
            nextLine()
        }

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
            if (isEnd()) {
                break
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
        return when (val string = readString()) {
            "define" -> Define(line, pos)
            "extern" -> Extern(line, pos)
            "void"   -> PrimitiveTypeToken("void", 0, line, pos)
            else -> {
                val begin = pos - string.length
                if (!isEnd() && getChar() == ':') {
                    nextChar()
                    LabelToken(string, line, begin)
                } else {
                    Identifier(string, line, begin)
                }
            }
        }
    }

    private fun readType(): TypeToken? {
        if (getChar() == '<') {
            nextChar()
            val tok = readType()!!
            skipWhitespace()

            assert(getChar() == 'x')
            nextChar()
            skipWhitespace()
            val begin = globalPosition
            while (!isEnd() && getChar().isDigit()) {
                nextChar()
            }
            val size = data.substring(begin, globalPosition)

            skipWhitespace()
            assert(getChar() == '>')
            nextChar()

            return ArrayTypeToken(size.toInt(), tok, line, pos)
        }

        val begin = globalPosition
        nextChar()
        val ch = getChar()

        if (!ch.isDigit()) {
            previousChar()
            return null
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
        return PrimitiveTypeToken(typeName, indirection, line, start)
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
        } else if (ch == 'u' || ch == 'i' || ch == 'f' || ch == '<') {
            readType() ?:
                readIdentifierOrKeywordOrType()
        } else if (ch.isLetter()) {
            readIdentifierOrKeywordOrType()
        } else if (ch == '%') {
            nextChar()
            val name = readString()
            val start = pos - 1 - name.length
            ValueInstructionToken(name, line, start)
        } else {
            throw RuntimeException("$line:$pos cannot parse: ${remainsLine(globalPosition)}")
        }
    }

    operator fun iterator(): TokenIterator {
        return TokenIterator(this)
    }
}