package tokenizer

import tokenizer.StringReader.Companion.tryPunct
import tokenizer.LexicalElements.isOperator2
import tokenizer.LexicalElements.isOperator3
import tokenizer.LexicalElements.keywords


class CTokenizer private constructor(private val filename: String, private val reader: StringReader) {
    private val tokens = TokenList()

    private var pos: Int = 1
    private var line: Int = 1

    fun doTokenize(): TokenList {
        doTokenizeHelper()
        return tokens
    }

    private fun incrementLine() {
        line += 1
        pos = 0
    }

    private fun eat(): Char {
        pos += 1
        return reader.read()
    }

    private fun eat(count: Int) {
        pos += count
        reader.read(count)
    }

    private fun append(next: AnyToken) {
        tokens.add(next)
    }

    private fun doTokenizeHelper() {
        while (!reader.eof) {
            val v = reader.peek()

            if (v == '\n') {
                eat()
                incrementLine()
                append(NewLine.of(1))
                continue
            }
            if (v.isWhitespace() || v == '\r') {
                var spaces = 1
                eat()
                while (!reader.eof && (reader.peek() == ' ' || reader.peek() == '\t')) {
                    eat()
                    spaces += 1
                }
                append(Indent.of(spaces))
                continue
            }
            if (v == '"' || v == '\'' || v == '`') {
                val literal = reader.readBlock {
                    eat()
                    while (!reader.eof && reader.peek() != v) {
                        val c = eat()
                        if (c == '\\') {
                            eat()
                        }
                    }
                    if (!reader.eof) {
                        eat()
                    }
                }
                append(StringLiteral(literal, OriginalPosition(line, pos - literal.length, filename)))
                continue
            }

            // Single line comments
            if (reader.tryPeek("//")) {
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
            if (reader.tryPeek("/*")) {
                eat(2)

                while (!reader.eof && !reader.tryPeek("*/")) {
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
                pos += 1
                if (reader.inRange(2) &&
                    isOperator3(v, reader.peekOffset(1), reader.peekOffset(2))) {
                    val operator = reader.peek(3)
                    reader.read(3)
                    pos += 2
                    append(Identifier(operator, OriginalPosition(line, pos - 3, filename)))
                } else if (reader.inRange(1) && isOperator2(v, reader.peekOffset(1))) {
                    val operator = reader.peek(2)
                    reader.read(2)
                    pos += 1
                    append(Identifier(operator, OriginalPosition(line, pos - 2, filename)))
                } else if (v == '\\' && reader.peekOffset(1) == '\n') {
                    reader.read(2)
                    pos += 1
                    incrementLine()
                } else {
                    append(Punct(reader.read(), OriginalPosition(line, pos - 1, filename)))
                }
                continue
            }

            if (reader.peek() == '_' || v.isLetter()) {
                val identifier = reader.readBlock {
                    while (reader.peek().isLetter() || reader.peek().isDigit() || reader.peek() == '_') {
                        reader.read()
                        pos += 1
                    }
                }

                if (keywords.contains(identifier)) {
                    append(Keyword(identifier, OriginalPosition(line, pos - identifier.length, filename)))
                } else {
                    append(Identifier(identifier, OriginalPosition(line, pos - identifier.length, filename)))
                }
                continue
            }

            val saved = reader.pos
            val number = reader.readNumeric()

            when {
                number != null -> {
                    val diff = reader.pos - saved
                    pos += diff
                    append(Numeric(number, OriginalPosition(line, pos - diff, filename)))
                }
                else -> when {
                    else -> error("Unknown symbol: '$v' in '$filename' at $line:$pos")
                }
            }
        }
    }

    companion object {
        fun apply(file: StringReader, filename: String): TokenList {
            return CTokenizer(filename, file).doTokenize()
        }

        fun apply(data: String, filename: String): TokenList {
            return apply(StringReader(data), filename)
        }

        fun apply(data: String): TokenList {
            return apply(StringReader(data), "<no-name>")
        }
    }
}