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

    private fun isEndOfLine(): Boolean {
        return reader.check('\\') && reader.peekOffset(1) == '\n'
    }

    private fun readLiteral(quote: Char): String {
        eat()
        val builder = StringBuilder()

        while (!reader.check(quote)) {
            builder.append(eat())
            if (isEndOfLine()) {
                builder.append(eat())
                builder.append(eat())
                incrementLine()
            }
            if (reader.check('\\') && reader.peekOffset(1) == '\"') {
                eat()
                builder.append(eat())
            }
        }

        if (!reader.eof) {
            eat()
        }
        return builder.toString()
    }

    fun readEscapedChar(): Char {
        if (reader.peek() != '\\') {
            throw IllegalStateException("Expected escape character")
        }
        reader.read()
        return when (reader.peek()) {
            'n' -> '\n'
            't' -> '\t'
            'r' -> '\r'
            '0' -> '\u0000'
            '\'' -> '\''
            '\\' -> '\\'
            else -> throw IllegalStateException("Unknown escape character")
        }
    }

    fun readCharLiteral(): Char {
        if (reader.peek() == '\\') {
            val ch = readEscapedChar()
            reader.read()
            if (reader.peek() != '\'') {
                throw IllegalStateException("Expected closing quote")
            }
            reader.read()
            return ch
        } else {
            val ch = reader.read()
            if (reader.peek() != '\'') {
                throw IllegalStateException("Expected closing quote")
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

    fun readPPNumber(): Pair<String, Int>? {
        return tryRead {
            val start = reader.pos
            if (reader.peek() == '0' && (reader.peekOffset(1) == 'x' || reader.peekOffset(1) == 'X')) {
                reader.read()
                reader.read()
                while (!reader.eof && (reader.peek().isDigit() || reader.peek().lowercaseChar() in 'a'..'f')) {
                    reader.read()
                }
                val withPostfix = tryGetSuffix(start + 2)
                if (withPostfix != null) {
                    return@tryRead Pair(withPostfix, 16)
                }
                val hexString = reader.str.substring(start + 2, reader.pos)
                return@tryRead Pair(hexString, 16)
            }

            reader.read()
            while (!reader.eof &&
                (reader.peek().isDigit() ||
                        reader.peek() == 'e' ||
                        reader.peek() == 'E' ||
                        reader.peek() == 'p' ||
                        reader.peek() == 'P')) {
                reader.read()
            }
            if (reader.eof) {
                return@tryRead Pair(reader.str.substring(start, reader.pos), 10)
            }

            if (reader.check('.') && (reader.eof(1) ||
                        reader.peekOffset(1).isWhitespace() ||
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
    }

    fun readIdentifier(): String {
        val startPos = reader.pos
        while (!reader.eof && (reader.peek().isLetter() || reader.peek().isDigit() || reader.check('_'))) {
            reader.read()
        }
        return reader.str.substring(startPos, reader.pos)
    }

    private fun readSpaces(): Int {
        var spaces = 0
        while (!reader.eof && reader.isSpace()) {
            eat()
            spaces += 1
        }
        return spaces
    }

    private fun doTokenizeHelper() {
        while (!reader.eof) {
            val v = reader.peek()
            if (isEndOfLine()) {
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
                append(CharLiteral(literal, OriginalPosition(line, position - 1, filename)))
                continue
            }
            if (reader.check('"')) {
                val literal = readLiteral(v)
                append(StringLiteral(literal, OriginalPosition(line, position - (literal.length + 2), filename)))
                continue
            }

            // Single line comments
            if (reader.check("//")) {
                eat(2)
                while (!reader.eof) {
                    if (reader.peek() == '\n') {
                        break
                    }
                    eat()
                }
                continue
            }

            // Multi line comments
            if (reader.check("/*")) {
                eat(2)

                while (!reader.eof && !reader.check("*/")) {
                    if (reader.peek() != '\n') {
                        eat()
                        continue
                    }
                    eat()
                    incrementLine()
                }
                if (!reader.eof) {
                    eat(2)
                }
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
            if (tryPunct(v)) {
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
                } else if (v == '\\' && reader.peekOffset(1) == '\n') {
                    reader.read(2)
                    position += 1
                    incrementLine()
                } else {
                    append(Punctuator(reader.read().toString(), OriginalPosition(line, position - 1, filename)))
                }
                continue
            }

            if (reader.check('_') || v.isLetter()) {
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
            val pair = readPPNumber()

            when {
                pair != null -> {
                    val diff = reader.pos - saved
                    position += diff
                    append(PPNumber(pair.first,pair.second, OriginalPosition(line, position - diff, filename)))
                }
                else -> error("Unknown symbol: '$v' in '$filename' at $line:$position")
            }
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