package ir.read

import ir.read.tokens.*
import ir.types.Type

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

    private fun isEnd(offset: Int): Boolean {
        return globalPosition + offset >= data.length
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
        if (isEnd()) {
            throw EOFException("$line:$pos unexpected EOF")
        }
        return data[globalPosition]
    }

    private fun getChar(offset: Int): Char {
        if (isEnd(offset)) {
            throw EOFException("$line:$pos unexpected EOF")
        }
        return data[globalPosition + offset]
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
        while (!isEnd() && isStringSymbol(getChar())) {
            nextChar()
        }

        val end = globalPosition
        return data.substring(begin, end)
    }

    private fun readIdentifierOrKeywordOrType(): Token {
        val begin = pos
        return when (val string = readString()) {
            "define"   -> Define(line, begin)
            "extern"   -> Extern(line, begin)
            "void"     -> VoidTypeToken(line, begin)
            "to"       -> To(line, begin)
            "ptr"      -> PointerTypeToken(line, begin)
            "constant" -> ConstantKeyword(line, begin)
            "global"   -> GlobalKeyword(line, begin)
            "type"     -> TypeKeyword(line, begin)
            "true"     -> BoolValueToken(true, line, begin)
            "false"    -> BoolValueToken(false, line, begin)
            "null"     -> NULLValueToken(line, begin)
            "label"    -> {
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
        return when (typeName) {
            "u1"  -> BooleanTypeToken(line, start)
            "u8"  -> UnsignedIntegerTypeToken(Type.U8, line, start)
            "u16" -> UnsignedIntegerTypeToken(Type.U16, line, start)
            "u32" -> UnsignedIntegerTypeToken(Type.U32, line, start)
            "u64" -> UnsignedIntegerTypeToken(Type.U64, line, start)
            "i8"  -> SignedIntegerTypeToken(Type.I8, line, start)
            "i16" -> SignedIntegerTypeToken(Type.I16, line, start)
            "i32" -> SignedIntegerTypeToken(Type.I32, line, start)
            "i64" -> SignedIntegerTypeToken(Type.I64, line, start)
            "f32" -> FloatTypeToken(Type.F32, line, start)
            "f64" -> FloatTypeToken(Type.F64, line, start)
            else -> throw TokenizerException("$line:$pos unknown type: '$typeName'")
        }
    }

    private fun readNumeric(begin: Int = globalPosition): AnyValueToken? {
        val pos = pos
        nextChar()
        while (!isEnd() && getChar().isDigit()) {
            nextChar()
        }

        if (getChar() == '.') {
            //Floating point value
            nextChar()
            while (!isEnd() && !isSeparator(getChar())) {
                nextChar()
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

    private fun readStringLiteral(): StringLiteralToken {
        nextChar()
        val begin = globalPosition
        var end = globalPosition
        while (getChar() != '"') {
            end += 1
            nextChar()
        }
        nextChar()
        val string = data.substring(begin, end)
        return StringLiteralToken(string, line, begin)
    }

    private fun readSymbolName(): SymbolValue {
        nextChar()
        val begin = pos
        val name = readString()
        return SymbolValue(name, line, begin)
    }

    private fun readTupleType(): TupleTypeToken {
        nextChar()
        val begin = pos
        val types = arrayListOf<TypeToken>()
        while (getChar() != '|') {
            val type = readType() ?: throw TokenizerException("$line:$pos expect type")
            types.add(type)
            skipWhitespace()
            if (getChar() == '|') {
                break
            }
            if (getChar() != ',') {
                throw TokenizerException("$line:$pos expect ','")
            }
            nextChar()
            skipWhitespace()
        }
        nextChar()
        return TupleTypeToken(types, line, begin)
    }

    private fun readDotOrVararg(): Token {
        val begin = pos
        nextChar()
        if (getChar() == '.' && getChar(1) == '.') {
            nextChar()
            nextChar()
            return Vararg(line, begin)
        } else {
            return Dot(line, begin)
        }
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
            '[' -> OpenSquareBracket(line, pos)
            ']' -> CloseSquareBracket(line, pos)
            else -> null
        }
        if (tok != null) {
            nextChar()
            return tok
        }

        when (ch) {
            '"' -> return readStringLiteral()
            '@' -> return readSymbolName()
            '|' -> return readTupleType()
            '.' -> return readDotOrVararg()
            else -> {}
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
            LocalValueToken(name, line, start)
        } else if (ch == '$') {
            val start = pos
            val name = readValueString()
            StructDefinition(name, line, start)
        } else {
            throw TokenizerException("$line:$pos cannot parse: '${remainsLine(globalPosition)}'")
        }
    }

    operator fun iterator(): TokenIterator {
        return TokenIterator(this)
    }

    companion object {
        private val separators = arrayOf(
            '{',
            '}',
            '(',
            ')',
            '=',
            ',',
            ':',
            '[',
            ']',
            ' ',
            '\t',
            '\n'
        )

        private fun isStringSymbol(char: Char): Boolean {
            return char.isLetterOrDigit() || char == '_' || char == '.' || char == '-'
        }

        fun isSeparator(char: Char): Boolean {
            return separators.contains(char)
        }
    }
}