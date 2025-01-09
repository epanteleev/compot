package tokenizer

import tokenizer.tokens.*
import tokenizer.LexicalElements.keywords
import tokenizer.LexicalElements.isOperator2
import tokenizer.LexicalElements.isOperator3
import tokenizer.StringReader.Companion.isSeparator
import tokenizer.StringReader.Companion.tryPunct
import kotlin.math.pow


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

    private fun tryGetIntegerSuffix(reader: StringReader, radix: Int, start: Int, end: Int): Number? {
        if (reader.check("LLU") || reader.check("llu")) {
            reader.read(3)
            return reader.str.substring(start, end).toULongOrNull(radix)?.toLong()
        } else if (reader.check("ULL") || reader.check("ull")) {
            reader.read(3)
            return reader.str.substring(start, end).toULongOrNull(radix)?.toLong()
        } else if (reader.check("ll") || reader.check("LL")) {
            reader.read(2)
            return reader.str.substring(start, end).toULongOrNull(radix)?.toLong()
        } else if (reader.check("LU") || reader.check("lu")) {
            reader.read(2)
            return reader.str.substring(start, end).toULongOrNull(radix)?.toLong()
        } else if (reader.check("UL") || reader.check("ul")) {
            reader.read(2)
            return reader.str.substring(start, end).toULongOrNull(radix)?.toLong()
        } else if (reader.check("U") || reader.check("u")) {
            reader.read()
            return reader.str.substring(start, end).toULongOrNull(radix)?.toLong()
        } else if (reader.check("L") || reader.check("l")) {
            reader.read()
            return reader.str.substring(start, end).toULongOrNull(radix)?.toLong()
        }
        return reader.str.substring(start, end).toInt()
    }

    private fun tryGetFPSuffix(reader: StringReader, start: Int, end: Int): Double? {
        if (reader.check("F") || reader.check("f")) {
            reader.read()
            return reader.str.substring(start, end).toDoubleOrNull()
        } else if (reader.check("D") || reader.check("d")) {
            reader.read()
            return reader.str.substring(start, end).toDoubleOrNull()
        }
        return reader.str.substring(start, end).toDoubleOrNull()
    }

    private fun toNumberDefault(data: String, radix: Int): Number? = data.toByteOrNull(radix)
        ?: data.toIntOrNull(radix)
        ?: data.toLongOrNull(radix)
        ?: data.toULongOrNull(radix)?.toLong()
        ?: data.toDoubleOrNull()

    private fun readHexChar(): Char {
        eat()
        var c = 0
        while (reader.isHexDigit()) {
            val ch = eat()
            c = c * 16 + ch.digitToInt(16)
        }
        return c.toChar()
    }

    private fun tryParseExp(reader: StringReader): Long? {
        if (reader.check('e') || reader.check('E')) {
            reader.read()
            val start = reader.pos
            if (reader.check('+') || reader.check('-')) {
                reader.read()
            }
            while (reader.isDigit()) {
                reader.read()
            }
            val exp = reader.str.substring(start, reader.pos)
            return exp.toLongOrNull()
        }
        return null
    }

    private fun tryParsePower(reader: StringReader): Long? {
        if (reader.check('p') || reader.check('P')) {
            reader.read()
            if (reader.check('+') || reader.check('-')) {
                reader.read()
            }
            val start = reader.pos
            while (reader.isDigit()) {
                reader.read()
            }
            val exp = reader.str.substring(start, reader.pos)
            return exp.toLongOrNull()
        }
        return null
    }

    private fun readPPNumber(string: String): Number? {
        val reader = StringReader(string)
        var base = 10

        val start: Int
        if (reader.check("0x") || reader.check("0X")) {
            reader.read(2)
            base = 16

            start = reader.pos
            do {
                reader.read()
            } while (reader.isHexDigit())

        } else if (reader.check("0b") || reader.check("0B")) {
            reader.read(2)
            base = 2

            start = reader.pos
            do {
                reader.read()
            } while (reader.isBinary())

        } else if (reader.check("0") && reader.isDigit(1)) {
            reader.read()
            base = 8

            start = reader.pos
            do {
                reader.read()
            } while (reader.isOctal())

        } else {

            start = reader.pos
            do {
                reader.read()
            } while (reader.isDigit())
        }

        if (reader.eof) {
            return toNumberDefault(reader.str.substring(start, reader.pos), base)
        }

        if (reader.check('.')) {
            //Floating point value
            reader.read()
            while (!reader.eof && reader.isDigit()) {
                reader.read()
            }
            val end = reader.pos

            val exponent = tryParseExp(reader)
            val fp = tryGetFPSuffix(reader, start, end) ?: return null //TODO return null is problem
            if (exponent != null) {
                return 10.0.pow(exponent.toDouble()) * fp
            }

            return fp
        }

        val end = reader.pos
        val exponent = tryParseExp(reader)
        if (exponent != null) {
            val fp = tryGetFPSuffix(reader, start, end) ?: return null //TODO return null is problem
            return 10.0.pow(exponent.toDouble()) * fp
        }

        val power = tryParsePower(reader)
        val num = tryGetIntegerSuffix(reader, base, start, end)?: tryGetFPSuffix(reader, start, reader.pos) ?: return null
        if (power != null) {
            return num.toDouble() * 2.0.pow(power.toDouble())
        }
        return num
    }

    private fun readIdentifier(): String {
        val startPos = reader.pos
        while (!reader.eof && (reader.isLetter() || reader.isDigit())) {
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
            if (reader.isDigit() || ((reader.check('.') && reader.isDigit(1)))) {
                val saved = position
                val numberStringBuilder = StringBuilder()
                numberStringBuilder.append(eat())

                while (true) {
                    if (reader.isDigit()) {
                        numberStringBuilder.append(eat())
                    } else if (reader.isOneFrom('e', 'E') && reader.isOneFrom(1, '+', '-')) {
                        numberStringBuilder.append(eat())
                        numberStringBuilder.append(eat())
                    } else if (reader.isOneFrom('p', 'P') && reader.isOneFrom(1, '+', '-')) {
                        numberStringBuilder.append(eat())
                        numberStringBuilder.append(eat())
                    } else if (reader.check('.')) {
                        numberStringBuilder.append(eat())
                    } else if (reader.isLetter()) {
                        numberStringBuilder.append(eat())
                    } else {
                        break
                    }
                }

                val numberString = numberStringBuilder.toString()
                val pair = readPPNumber(numberString)
                if (pair == null) {
                    append(Identifier(numberString, OriginalPosition(line, saved, filename)))
                    continue
                }

                append(PPNumber(numberString, pair, OriginalPosition(line, saved, filename)))
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