package tokenizer

import tokenizer.tokens.*
import tokenizer.LexicalElements.keywords
import tokenizer.LexicalElements.isOperator2
import tokenizer.LexicalElements.isOperator3
import tokenizer.StringReader.Companion.isSeparator
import tokenizer.StringReader.Companion.tryPunct


class CTokenizer private constructor(private val filename: String, private val reader: StringReader) {
    private val tokens = TokenList()
    private var position: Int = 1
    private var line: Int = 1

    fun doTokenize(): TokenList {
        doTokenizeHelper()
        return tokens
    }

    private fun incrementLine() {
        line += 1
        position = 0
    }

    private fun eat(): Char {
        position += 1
        return reader.read()
    }

    private fun eat(count: Int) {
        position += count
        reader.read(count)
    }

    private fun append(next: AnyToken) {
        tokens.add(next)
    }

    private fun asControlChar(ch: Char): Char? = when (ch) {
        'a' -> '\u0007'
        'b' -> '\b'
        't' -> '\t'
        'n' -> '\n'
        'v' -> '\u000B'
        'f' -> '\u000C'
        'r' -> '\r'
        '0' -> '\u0000'
        '\'' -> '\''
        '\\' -> '\\'
        '"' -> '"'
        else -> null
    }

    private fun readLiteral(quote: Char): String {
        eat()
        val builder = StringBuilder()

        while (!reader.check(quote)) {
            if (reader.check("\\\n")) {
                eat()
                eat()
                incrementLine()
            }
            if (reader.check("\\\"")) {
                eat()
                builder.append(eat())
                continue
            }
            if (reader.check("\\\\")) {
                builder.append(eat())
                builder.append(eat())
                continue
            }
            if (reader.check('\\')) {
                eat()
                val ch = asControlChar(reader.peek())
                if (ch != null) {
                    builder.append(ch)
                    eat()
                    continue
                }

                if (reader.check('x')) {
                    builder.append("\\")
                    eatHexChar(builder)
                    continue
                }
                throw IllegalStateException("Unknown escape sequence in line $line")
            }

            builder.append(eat())
        }

        if (!reader.eof) {
            eat()
        }
        return builder.toString()
    }

    private fun eatHexChar(builder: java.lang.StringBuilder) {
        builder.append(eat())
        while (reader.isHexDigit()) {
            builder.append(eat())
        }
    }

    private fun readEscapedChar(): Char {
        eat()

        val ch = asControlChar(reader.peek())
        if (ch != null) {
            eat()
            return ch
        }

        if (reader.check('x')) {
            return readHexChar()
        }
        return eat()
    }

    private fun readCharLiteral(): Char {
        val ch = if (reader.check('\\')) {
            readEscapedChar()
        } else {
            eat()
        }

        if (!reader.check('\'')) {
            throw IllegalStateException("Expected closing quote in line $line")
        }
        eat()
        return ch
    }

    private fun tryGetSuffix(start: Int): String? {
        if (reader.check("LLU") || reader.check("llu")) {
            eat(3)
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("ULL") || reader.check("ull")) {
            eat(3)
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("ll") || reader.check("LL")) {
            eat(2)
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("LU") || reader.check("lu")) {
            eat(2)
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("UL") || reader.check("ul")) {
            eat(2)
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("U") || reader.check("u")) {
            eat()
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("L") || reader.check("l")) {
            eat()
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("F") || reader.check("f")) {
            eat()
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("D") || reader.check("d")) {
            eat()
            return reader.str.substring(start, reader.pos)
        }
        return null
    }

    private inline fun <T> tryRead(callback: () -> T?): T? {
        val old = reader.pos
        val result = callback()
        if (result == null) {
            reader.pos = old
        }
        return result
    }

    private fun readHexChar(): Char {
        eat()
        var c = 0
        while (reader.isHexDigit()) {
            val ch = eat()
            c = c * 16 + ch.digitToInt(16)
        }
        return c.toChar()
    }

    private fun readPPNumber(): Pair<String, Int>? = tryRead {
        var base = 10
        if (reader.check("0x") || reader.check("0X")) {
            eat(2)
            base = 16
        } else if (reader.check("0b") || reader.check("0B")) {
            reader.read(2)
            base = 2
        } else if (reader.check("0") && reader.peekOffset(1).isDigit()) {
            eat()
            base = 8
        }
        val start = reader.pos

        do {
            eat()
            if (reader.eof) {
                return@tryRead Pair(reader.str.substring(start, reader.pos), base)
            }
        } while (reader.isHexDigit())

        if (reader.check('.')) {
            //Floating point value
            eat()
            while (!reader.eof && !isSeparator(reader.peek())) {
                eat()
            }
            val result = tryGetSuffix(start) ?: reader.str.substring(start, reader.pos)
            return@tryRead Pair(result, base)
        }
        if (!reader.isLetter()) {
            // Integer
            val result = tryGetSuffix(start) ?: reader.str.substring(start, reader.pos)
            return@tryRead Pair(result, base)
        }

        val postfix = tryGetSuffix(start)
        if (postfix != null) {
            return@tryRead Pair(postfix, base)
        }
        return@tryRead null
    }

    private fun readIdentifier(): String {
        val startPos = reader.pos
        while (!reader.eof && (reader.isLetter() || reader.peek().isDigit())) {
            eat()
        }
        return reader.str.substring(startPos, reader.pos)
    }

    private fun readSpaces(): Int {
        var spaces = 0
        while (reader.isSpace()) {
            eat()
            spaces += 1
        }
        return spaces
    }

    private fun skipComment() {
        while (!reader.eof) {
            if (reader.check('\n')) {
                break
            }
            eat()
        }
    }

    private fun skipMultilineComments() {
        while (!reader.eof) {
            if (reader.check("*/")) {
                eat(2)
                break
            }
            if (reader.check("*\\\n/")) {
                eat(4)
                incrementLine()
                break
            }
            if (reader.check('\n')) {
                eat()
                incrementLine()
                continue
            }
            eat()
        }
    }

    private fun doTokenizeHelper() {
        while (!reader.eof) {
            if (reader.check("\\\n")) {
                eat(2)
                incrementLine()
                continue
            }

            if (reader.check("\\\r\n")) { //todo normalize strings
                eat(3)
                incrementLine()
                continue
            }

            if (reader.check('\n')) {
                eat()
                incrementLine()
                append(NewLine.of(1))
                continue
            }
            if (reader.isSpace()) {
                val spaces = readSpaces()
                append(Indent.of(spaces))
                continue
            }

            // Character literals
            if (reader.check('\'')) {
                val start = position
                eat()
                val literal = readCharLiteral()
                append(CharLiteral(literal, OriginalPosition(line, start, filename)))
                continue
            }

            // String literals
            if (reader.check('"')) {
                val start = position
                val literal = readLiteral(reader.peek())
                append(StringLiteral(literal, OriginalPosition(line, start, filename)))
                continue
            }

            // Single line comments
            if (reader.check("//")) {
                eat(2)
                skipComment()
                continue
            }

            // Multi line comments
            if (reader.check("/*")) {
                eat(2)
                skipMultilineComments()
                continue
            }
            if (reader.check("/\\\n*")) {
                eat(4)
                incrementLine()
                skipMultilineComments()
                continue
            }

            // Numbers
            if (reader.peek().isDigit() || (reader.check('.') && reader.peekOffset(1).isDigit())) {
                val saved = position
                val v = reader.peek()
                val pair = readPPNumber() ?: error("Unknown symbol: '$v' in '$filename' at $line:$position")

                append(PPNumber(pair.first, pair.second, OriginalPosition(line, saved, filename)))
                continue
            }

            // Punctuations
            if (tryPunct(reader.peek())) {
                val v = reader.peek()
                val saved = position
                if (reader.inRange(2) &&
                    isOperator3(v, reader.peekOffset(1), reader.peekOffset(2))) {
                    val operator = reader.peek(3)
                    eat(3)
                    append(Punctuator(operator, OriginalPosition(line, saved, filename)))
                } else if (reader.inRange(1) && isOperator2(v, reader.peekOffset(1))) {
                    val operator = reader.peek(2)
                    eat(2)
                    append(Punctuator(operator, OriginalPosition(line, saved, filename)))
                } else if (reader.check("\\\n")) {
                    eat(2)
                    incrementLine()
                } else {
                    val s = eat()
                    append(Punctuator(s.toString(), OriginalPosition(line, saved, filename)))
                }
                continue
            }

            // Keywords or identifiers
            if (reader.isLetter()) {
                val saved = position
                val identifier = readIdentifier()
                if (keywords.contains(identifier)) {
                    append(Keyword(identifier, OriginalPosition(line, saved, filename)))
                } else {
                    append(Identifier(identifier, OriginalPosition(line, saved, filename)))
                }
                continue
            }

            error("Unknown symbol: '${reader.peek()}' in '$filename' at $line:$position")
        }
    }

    companion object {
        fun apply(data: String, filename: String): TokenList {
            return CTokenizer(filename, StringReader(data)).doTokenize()
        }

        fun apply(data: String): TokenList {
            return CTokenizer("<no-name>", StringReader(data)).doTokenize()
        }
    }
}