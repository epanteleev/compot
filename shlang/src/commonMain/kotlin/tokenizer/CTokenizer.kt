package tokenizer

import tokenizer.StringReader.Companion.tryPunct
import tokenizer.LexicalElements.isOperator2
import tokenizer.LexicalElements.isOperator3
import tokenizer.LexicalElements.keywords


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

    private fun isBackSlash(): Boolean {
        return reader.check('\\') && reader.peekOffset(1) == '\n'
    }

    private fun rearLiteral(quote: Char): String {
        eat()
        val literal = reader.readBlock {
            while (!reader.check(quote)) {
                eat()
                if (isBackSlash()) {
                    eat(2)
                    incrementLine()
                }
            }
        }
        if (!reader.eof) {
            eat()
        }
        return literal
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
            if (isBackSlash()) {
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
                val literal = reader.readCharLiteral()
                append(CharLiteral(literal, OriginalPosition(line, position - 1, filename)))
                continue
            }
            if (reader.check('"')) {
                val literal = rearLiteral(v)
                val last = tokens.lastNotSpace()
                if (last is StringLiteral) {
                    tokens.remove(last)
                    append(StringLiteral(last.unquote() + literal, OriginalPosition(last.line(), last.pos(), filename)))
                } else {
                    append(StringLiteral(literal, OriginalPosition(line, position - (literal.length + 2), filename)))
                }

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

            // Punctuations and operators (or indentifiers)
            if (tryPunct(v)) {
                position += 1
                if (reader.inRange(2) &&
                    isOperator3(v, reader.peekOffset(1), reader.peekOffset(2))) {
                    val operator = reader.peek(3)
                    reader.read(3)
                    position += 2
                    append(Identifier(operator, OriginalPosition(line, position - 3, filename)))
                } else if (reader.inRange(1) && isOperator2(v, reader.peekOffset(1))) {
                    val operator = reader.peek(2)
                    reader.read(2)
                    position += 1
                    append(Identifier(operator, OriginalPosition(line, position - 2, filename)))
                } else if (v == '\\' && reader.peekOffset(1) == '\n') {
                    reader.read(2)
                    position += 1
                    incrementLine()
                } else {
                    append(Punct(reader.read(), OriginalPosition(line, position - 1, filename)))
                }
                continue
            }

            if (reader.check('_') || v.isLetter()) {
                val identifier = reader.readIdentifier()
                position += identifier.length

                if (keywords.contains(identifier)) {
                    append(Keyword(identifier, OriginalPosition(line, position - identifier.length, filename)))
                } else {
                    append(Identifier(identifier, OriginalPosition(line, position - identifier.length, filename)))
                }
                continue
            }

            val saved = reader.pos
            val pair = reader.readCNumber()

            when {
                pair != null -> {
                    val diff = reader.pos - saved
                    position += diff
                    append(Numeric(pair.first,pair.second, OriginalPosition(line, position - diff, filename)))
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