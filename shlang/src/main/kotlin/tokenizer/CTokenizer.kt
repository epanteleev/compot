package tokenizer

import tokenizer.StringReader.Companion.tryPunct
import tokenizer.Specifiers.isOperator2
import tokenizer.Specifiers.isOperator3


class CTokenizer private constructor(private val filename: String, private val reader: StringReader) {
    private var out: Token? = null
    private var first: AnyToken = Eof

    private var pos: Int = 1
    private var line: Int = 1

    fun doTokenize(): AnyToken {
        doTokenizeHelper()
        return first
    }

    private fun incrementLine() {
        line += 1
        pos = 1
    }

    private fun eat(): Char {
        pos += 1
        return reader.read()
    }

    private fun eat(count: Int) {
        pos += count
        reader.read(count)
    }

    private fun append(next: Token) {
        if (out == null) {
            out = next
            first = next
            return
        }
        out!!.next = next
        out = next
    }

    private fun doTokenizeHelper() {
        while (!reader.eof) {
            val v = reader.peek()

            if (v == '\n') {
                reader.read()
                incrementLine()
                continue
            }
            if (v.isWhitespace() || v == '\r') {
                eat()
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
                append(StringLiteral(literal, line, pos - literal.length, filename))
                continue
            }

            // Single line comments
            if (reader.tryPeek("//")) {
                eat()
                while (!reader.eof || reader.peek() == '\n') {
                    pos += 1
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
                    incrementLine()
                }
                if (!reader.eof) {
                    eat(2)
                }
                continue
            }

            if (tryPunct(v)) {
                pos += 1
                if (reader.inRange(2) &&
                    isOperator3(v, reader.peekOffset(1), reader.peekOffset(2))) {
                    val operator = reader.peek(3)
                    reader.read(3)
                    pos += 2
                    append(Ident(operator, line, pos - 3, filename))
                } else if (reader.inRange(1) && isOperator2(v, reader.peekOffset(1))) {
                    val operator = reader.peek(2)
                    reader.read(2)
                    pos += 1
                    append(Ident(operator, line, pos - 2, filename))
                } else {
                    append(Punct(reader.read(), line, pos - 1, filename))
                }
                continue
            }

            if (v.isLetter()) {
                val identifier = reader.readBlock {
                    while (reader.peek().isLetter()) {
                        reader.read()
                        pos += 1
                    }
                }
                append(Ident(identifier, line, pos - identifier.length, filename))
                continue
            }

            val saved = reader.pos
            val number = reader.readNumeric()

            when {
                number != null -> {
                    val diff = reader.pos - saved
                    pos += diff
                    append(Numeric(number, line, pos - diff, filename))
                }
                else -> when {
                    else -> error("Unknown symbol: '$v'")
                }
            }
        }
    }

    companion object {
        fun apply(file: StringReader): AnyToken {
            return CTokenizer("<no-name>", file).doTokenize()
        }

        fun apply(data: String): AnyToken {
            return apply(StringReader(data))
        }
    }
}