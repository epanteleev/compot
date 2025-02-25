package tokenizer

import tokenizer.tokens.*
import tokenizer.LexicalElements.keywords
import tokenizer.LexicalElements.isOperator2
import tokenizer.LexicalElements.isOperator3
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

            if (reader.check('\\')) {
                eat()
                val ch = asControlChar(reader.peek())
                if (ch != null) {
                    builder.append(ch)
                    eat()
                    continue
                }

                if (reader.check('x')) {
                    eatHexChar(builder)
                    continue
                }
                var octal = 0
                while (reader.isOctal()) {
                    octal = octal * 8 + eat().digitToInt(8)
                }

                builder.append(octal.toChar())
            }

            builder.append(eat())
        }

        if (!reader.eof) {
            eat()
        }
        return builder.toString()
    }

    private fun eatHexChar(builder: StringBuilder) {
        eat()
        var c = 0
        while (reader.isHexDigit()) {
            val ch = eat()
            c = c * 16 + ch.digitToInt(16)
        }
        val res = c and 0xFF
        builder.append(res.toChar())
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

    private fun tryGetFPSuffix(str: String): Double? = try {
        java.lang.Double.valueOf(str)
    } catch (_: NumberFormatException) {
        null
    }

    private fun toNumberDefault(data: String, radix: Int): Number? = data.toByteOrNull(radix)
        ?: data.toIntOrNull(radix)
        ?: data.toLongOrNull(radix)
        ?: data.toULongOrNull(radix)?.toLong()
        ?: data.toDoubleOrNull()

    private fun toLongDefault(data: String, radix: Int): Number? = data.toLongOrNull(radix)
        ?: data.toULongOrNull(radix)?.toLong()
        ?: data.toDoubleOrNull()

    private fun readHexChar(): Char {
        eat()
        var c = 0
        while (reader.isHexDigit()) {
            val ch = eat()
            c = c.toByte() * 16 + ch.digitToInt(16)
        }
        return c.toChar()
    }

    private fun isFPSuffix(str: String): Boolean {
        if (str.isEmpty()) {
            return false
        }
        val last = str.last()
        return when (last) {
            'f', 'F', 'd', 'D' -> true
            else -> false
        }
    }

    private fun isLongSuffix1(str: String): Boolean {
        if (str.isEmpty()) {
            return false
        }
        val last = str.last()
        return when (last) {
            'u', 'U', 'l', 'L' -> true
            else -> false
        }
    }

    private fun isLongSuffix2(string: String): Boolean = string.endsWith("ul") ||
        string.endsWith("UL") ||
        string.endsWith("lu") ||
        string.endsWith("LU") ||
        string.endsWith("ll") ||
        string.endsWith("LL")

    private fun isLongSuffix3(string: String): Boolean = string.endsWith("ull") ||
        string.endsWith("ULL") ||
        string.endsWith("llu") ||
        string.endsWith("LLU")

    private fun numberSubstring(string: String, base: Int, last: Int): String = when (base) {
        16 -> string.substring(2, string.length - last)
        10 -> string.substring(0, string.length - last)
        8 -> string.substring(1, string.length - last)
        2 -> string.substring(2, string.length - last)
        else -> throw IllegalStateException("Unknown base")
    }

    private fun numberWithPower(string: String, base: Int): Number? {
        if (base == 10) {
            if (string.contains('e') || string.contains('E')) {
                return java.lang.Double.valueOf(string)
            }
        }
        if (base == 16) {
            if (string.contains('p') || string.contains('P')) {
                return java.lang.Double.valueOf(string)
            }
        }
        val substring = numberSubstring(string, base, 0)
        return toLongDefault(substring, base)
    }

    private fun readPPNumber(string: String): Number? {
        var base = 10

        if (string.startsWith("0x") || string.startsWith("0X")) {
            base = 16
        } else if (string.startsWith("0b") || string.startsWith("0B")) {
            base = 2
        } else if (string.startsWith("0") && string.getOrNull(1)?.isDigit() == true) {
            base = 8
        }

        if (string.contains('.')) {
            if (isFPSuffix(string)) {
                val substring = string.substring(0, string.length - 1)
                return tryGetFPSuffix(substring)
            }

            if (isLongSuffix1(string)) {
                val substring = numberSubstring(string, base, 1)
                return toLongDefault(substring, base)
            }

            return tryGetFPSuffix(string)
        }

        if (isLongSuffix3(string)) {
            val substring = numberSubstring(string, base, 3)
            return toLongDefault(substring, base)
        }

        if (isLongSuffix2(string)) {
            val substring = numberSubstring(string, base, 2)
            return toLongDefault(substring, base)
        }

        if (isLongSuffix1(string)) {
            val substring = string.substring(0, string.length - 1)
            return numberWithPower(substring, base)
        }

        if (base == 10) {
            if (string.contains('e') || string.contains('E')) {
                return java.lang.Double.valueOf(string)
            }
        }

        if (base == 16) {
            if (string.contains('p') || string.contains('P')) {
                return java.lang.Double.valueOf(string)
            }
        }

        val substring = numberSubstring(string, base, 0)
        return toNumberDefault(substring, base)
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
    }
}