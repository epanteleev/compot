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
                val ch = when (reader.peek()) {
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
        reader.read()

        val ch = when (reader.peek()) {
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

        if (ch != null) {
            reader.read()
            return ch
        }

        if (reader.check('x')) {
            return readHexChar()
        }
        return reader.read()
    }

    private fun readCharLiteral(): Char {
        val ch = if (reader.check('\\')) {
            readEscapedChar()
        } else {
            reader.read()
        }

        if (!reader.check('\'')) {
            throw IllegalStateException("Expected closing quote in line $line")
        }
        reader.read()
        return ch
    }

    private fun tryGetSuffix(start: Int): String? {
        if (reader.check("LLU") || reader.check("llu")) {
            reader.read(3)
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("ULL") || reader.check("ull")) {
            reader.read(3)
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("ll") || reader.check("LL")) {
            reader.read(2)
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("LU") || reader.check("lu")) {
            reader.read(2)
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("UL") || reader.check("ul")) {
            reader.read(2)
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("U") || reader.check("u")) {
            reader.read()
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("L") || reader.check("l")) {
            reader.read()
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("F") || reader.check("f")) {
            reader.read()
            return reader.str.substring(start, reader.pos)
        } else if (reader.check("D") || reader.check("d")) {
            reader.read()
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
        reader.read()
        var c = 0
        while (reader.isHexDigit()) {
            val ch = reader.read()
            c = c * 16 + ch.digitToInt(16)
        }
        return c.toChar()
    }

    private fun readPPNumber(): Pair<String, Int>? = tryRead {
        var base = 10
        if (reader.check("0x") || reader.check("0X")) {
            reader.read(2)
            base = 16
        } else if (reader.check("0b") || reader.check("0B")) {
            reader.read(2)
            base = 2
        } else if (reader.check("0") && reader.peekOffset(1).isDigit()) {
            reader.read()
            base = 8
        }
        val start = reader.pos

        do {
            reader.read()
            if (reader.eof) {
                return@tryRead Pair(reader.str.substring(start, reader.pos), base)
            }
        } while (reader.isHexDigit())

        if (reader.check('.')) {
            //Floating point value
            reader.read()
            while (!reader.eof && !isSeparator(reader.peek())) {
                reader.read()
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
            reader.read()
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
                reader.read()
                val literal = readCharLiteral()
                append(CharLiteral(literal, OriginalPosition(line, position, filename)))
                continue
            }

            // String literals
            if (reader.check('"')) {
                val start = reader.pos
                val literal = readLiteral(reader.peek())
                val diff = reader.pos - start
                append(StringLiteral(literal, OriginalPosition(line, position - diff, filename)))
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
                val saved = reader.pos
                val v = reader.peek()
                val pair = readPPNumber() ?: error("Unknown symbol: '$v' in '$filename' at $line:$position")

                val diff = reader.pos - saved
                position += diff
                append(PPNumber(pair.first,pair.second, OriginalPosition(line, position - diff, filename)))
                continue
            }

            // Punctuations
            if (tryPunct(reader.peek())) {
                val v = reader.peek()
                position += 1
                if (reader.inRange(2) &&
                    isOperator3(v, reader.peekOffset(1), reader.peekOffset(2))) {
                    val operator = reader.peek(3)
                    reader.read(3)
                    position += 2
                    append(Punctuator(operator, OriginalPosition(line, position - 3, filename)))
                } else if (reader.inRange(1) && isOperator2(v, reader.peekOffset(1))) {
                    val operator = reader.peek(2)
                    reader.read(2)
                    position += 1
                    append(Punctuator(operator, OriginalPosition(line, position - 2, filename)))
                } else if (reader.check("\\\n")) {
                    reader.read(2)
                    position += 1
                    incrementLine()
                } else {
                    append(Punctuator(reader.read().toString(), OriginalPosition(line, position - 1, filename)))
                }
                continue
            }

            // Keywords or identifiers
            if (reader.isLetter()) {
                val identifier = readIdentifier()
                position += identifier.length

                if (keywords.contains(identifier)) {
                    append(Keyword(identifier, OriginalPosition(line, position - identifier.length, filename)))
                } else {
                    append(Identifier(identifier, OriginalPosition(line, position - identifier.length, filename)))
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