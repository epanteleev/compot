package ir.read

class EOFException(message: String): Exception(message)
class TokenizerException(message: String): Exception(message)

class Tokenizer(val data: String) {
    private var globalPosition = 0
    private var line = 1
    private var pos = 0

    private fun remainsLine(begin: Int): String {
        val end = data.indexOf('\n', begin)
        return if (end == -1) {
            data.substring(begin, data.length)
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
        if (globalPosition >= data.length) {
            throw EOFException("unexpected EOF in $line, $pos")
        }
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

    private fun readString(begin: Int = globalPosition): String {
        nextChar()
        while (!isEnd() && (getChar().isLetterOrDigit() || getChar() == '_' || getChar() == '.')) {
            nextChar()
        }
        val end = globalPosition
        return data.substring(begin, end)
    }

    private fun readIdentifierOrKeywordOrType(): Token {
        val begin = pos
        return when (val string = readString()) {
            "define" -> Define(line, begin)
            "extern" -> Extern(line, begin)
            "void"   -> ElementaryTypeToken("void", line, begin)
            "to"     -> To(line, begin)
            "ptr"    -> ElementaryTypeToken("ptr", line, begin)
            "label"  -> {
                skipWhitespace()
                if (getChar() != '%') {
                    throw TokenizerException("$line:$pos cannot parse: '${remainsLine(globalPosition)}'")
                }
                val labelName = readValueString()
                LabelUsage(labelName, line, begin)
            }
            else -> {
                if (!isEnd() && getChar() == ':') {
                    nextChar()
                    LabelDefinition(string, line, begin)
                } else {
                    Identifier(string, line, begin)
                }
            }
        }
    }

    private fun readArrayType(): ArrayTypeToken {
        nextChar()
        val tok = readType()!!
        skipWhitespace()

        if (getChar() != 'x') {
            throw TokenizerException("expect 'x' symbol")
        }
        nextChar()
        skipWhitespace()
        val size = readNumeric() ?: throw TokenizerException("expect size of array")
        if (size !is IntValue) {
            throw TokenizerException("expect integer type of array size")
        }

        skipWhitespace()
        if (getChar() != '>') {
            throw TokenizerException("expect '>' symbol")
        }
        nextChar()

        return ArrayTypeToken(size.int, tok, line, pos)
    }

    private fun readType(): TypeToken? {
        if (getChar() == '<') {
            return readArrayType()
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
        val start = pos - typeName.length
        return ElementaryTypeToken(typeName, line, start)
    }

    private fun readNumeric(begin: Int = globalPosition): ValueToken? {
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
                throw TokenizerException("$line:$pos cannot parse floating point value: '${remainsLine(begin)}'")
            }
            val floatString = data.substring(begin, globalPosition)
            val float = floatString.toDoubleOrNull()
                ?: throw TokenizerException("Cannot to be converted to floating point value: '$floatString'")

            return FloatValue(float, line, pos)

        } else if (!getChar().isLetter() && getChar() != '_') {
            // Integer
            val intString = data.substring(begin, globalPosition)
            val intOrNull = intString.toLongOrNull() ?:
                throw TokenizerException("$line:$pos cannot to be converted to integer: '$intString'")

            return IntValue(intOrNull, line, pos)
        } else {
            return null
        }
    }

    private fun readNumberOrIdentifier(): Token {
        val begin = globalPosition
        val pos = pos

        val number = readNumeric(begin)
        if (number != null) {
            return number
        }

        val string = readString(begin)
        return Identifier(string, line, pos)
    }

    private fun readValueString(): String {
        nextChar()
        return readString()
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

        if (ch == '@') {
            nextChar()
            val name = readString()
            return FunctionName(name, line, pos - name.length)
        }

        return if (ch == '-' || ch.isDigit()) {
            readNumberOrIdentifier()
        } else if (ch == 'u' || ch == 'i' || ch == 'f' || ch == '<') {
            readType() ?:
                readIdentifierOrKeywordOrType()
        } else if (ch.isLetter()) {
            readIdentifierOrKeywordOrType()
        } else if (ch == '%') {
            val start = pos
            val name = readValueString()
            ValueInstructionToken(name, line, start)
        } else {
            throw TokenizerException("$line:$pos cannot parse: ${remainsLine(globalPosition)}")
        }
    }

    operator fun iterator(): TokenIterator {
        return TokenIterator(this)
    }
}