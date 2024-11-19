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
            builder.append(eat())
        }

        if (!reader.eof) {
            eat()
        }
        return builder.toString()
    }

    private fun readEscapedChar(): Char {
        if (reader.peek() != '\\') {
            throw IllegalStateException("Expected escape character")
        }
        reader.read()

        val ch = when (reader.peek()) {
            'n' -> '\n'
            't' -> '\t'
            'r' -> '\r'
            '0' -> '\u0000'
            '\'' -> '\''
            '\\' -> '\\'
            'v' -> '\u000B'
            'f' -> '\u000C'
            'b' -> '\b'
            '"' -> '"'
            'a' -> '\u0007'
            else -> null
        }

        if (ch != null) {
            reader.read()
            return ch
        }

        if (reader.peek() == 'x') {
            reader.read()
            val hex = readHexNumber()
            return hex.toInt(16).toChar()
        }
        return reader.read()
    }

    private fun readCharLiteral(): Char {
        if (reader.peek() == '\\') {
            val ch = readEscapedChar()
            if (reader.peek() != '\'') {
                throw IllegalStateException("Expected closing quote in line $line")
            }
            reader.read()
            return ch
        } else {
            val ch = reader.read()
            if (reader.peek() != '\'') {
                throw IllegalStateException("Expected closing quote: '${reader.peek()}' in line $line")
            }
            reader.read()
            return ch
        }
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

    private fun readHexNumber(): String {
        val start = reader.pos
        while (!reader.eof && (reader.peek().isDigit() || reader.peek().lowercaseChar() in 'a'..'f')) {
            reader.read()
        }
        val withPostfix = tryGetSuffix(start)
        if (withPostfix != null) {
            return withPostfix
        }
        return reader.str.substring(start, reader.pos)
    }

    private fun readPPNumber(): Pair<String, Int>? = tryRead {
        val start = reader.pos
        if (reader.check("0x") || reader.check("0X")) {
            reader.read(2)
            val hexString = readHexNumber()
            return@tryRead Pair(hexString, 16)
        }

        do {
            reader.read()
            if (reader.eof) {
                return@tryRead Pair(reader.str.substring(start, reader.pos), 10)
            }
            val ch = reader.peek()
        } while (!reader.eof && (ch.isDigit() || ch == 'e' || ch == 'E' || ch == 'p' || ch == 'P'))

        if (reader.check('.') && (reader.eof(1) ||
                    reader.peekOffset(1).isWhitespace() ||
                    isSeparator(reader.peekOffset(1)) ||
                    reader.peekOffset(1).isDigit() ||
                    reader.peekOffset(1) == 'f' ||
                    reader.peekOffset(1) == 'F')) {
            //Floating point value
            reader.read()
            while (!reader.eof && !isSeparator(reader.peek())) {
                reader.read()
            }
            val result = tryGetSuffix(start) ?: reader.str.substring(start, reader.pos)
            return@tryRead Pair(result, 10)
        } else if (!reader.peek().isLetter() && reader.peek() != '_') {
            // Integer
            val result = tryGetSuffix(start) ?: reader.str.substring(start, reader.pos)
            return@tryRead Pair(result, 10)
        }

        val postfix = tryGetSuffix(start)
        if (postfix != null) {
            return@tryRead Pair(postfix, 10)
        }
        return@tryRead null
    }

    private fun readIdentifier(): String {
        val startPos = reader.pos
        while (!reader.eof && (reader.peek().isLetter() || reader.peek().isDigit() || reader.check('_'))) {
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
            if (reader.check('\'')) {
                reader.read()
                val literal = readCharLiteral()
                append(CharLiteral(literal, OriginalPosition(line, position, filename)))
                continue
            }
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

            if (reader.check('.') && reader.peekOffset(1).isDigit()) {
                val pair = readPPNumber()
                if (pair != null) {
                    val diff = reader.pos
                    position += diff
                    append(PPNumber(pair.first, pair.second, OriginalPosition(line, position - diff, filename)))
                }
                continue
            }

            // Punctuations and operators (or indentifiers)
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

            val v = reader.peek()
            if (v == '_' || v.isLetter()) {
                val identifier = readIdentifier()
                position += identifier.length

                if (keywords.contains(identifier)) {
                    append(Keyword(identifier, OriginalPosition(line, position - identifier.length, filename)))
                } else {
                    append(Identifier(identifier, OriginalPosition(line, position - identifier.length, filename)))
                }
                continue
            }

            val saved = reader.pos
            val pair = readPPNumber() ?: error("Unknown symbol: '$v' in '$filename' at $line:$position")

            val diff = reader.pos - saved
            position += diff
            append(PPNumber(pair.first,pair.second, OriginalPosition(line, position - diff, filename)))
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