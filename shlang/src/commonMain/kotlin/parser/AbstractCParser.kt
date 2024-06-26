package parser

import tokenizer.*
import common.AnyParser
import types.TypeHolder


abstract class AbstractCParser(iterator: MutableList<AnyToken>): AnyParser(iterator) {
    protected val typeHolder = TypeHolder.default()

    fun typeHolder(): TypeHolder = typeHolder

    protected fun eof(): Boolean {
        return eof(0)
    }

    protected fun eof(offset: Int): Boolean {
        if (current + offset >= tokens.size) {
            return true
        }
        var pos = current
        var tokensNumber = 0
        do {
            if (tokens[pos] is Eof) {
                return true
            }
            if (tokens[pos] is CToken) {
                tokensNumber += 1
            }
            if (tokensNumber == offset + 1) {
                return false
            }
            pos += 1
        } while (pos < tokens.size)
        return true
    }

    protected fun skipSpaces() {
        while (current < tokens.size && tokens[current] !is CToken) {
            current += 1
        }
    }

    protected fun eat() {
        skipSpaces()
        if (eof()) {
            throw ParserException(ProgramMessage("Unexpected EOF", peak()))
        }
        current += 1
    }

    protected inline fun<reified T: AnyToken> peak(): T {
        return peak(0)
    }

    protected inline fun<reified T: AnyToken> peak(offset: Int): T {
        skipSpaces()
        if (eof(offset)) {
            throw ParserException(ProgramMessage("Unexpected EOF", tokens[current]))
        }
        val tok = tokens[current + offset]
        if (tok !is T) {
            throw ParserException(ProgramMessage("Unexpected token $tok", tok))
        }
        return tok
    }

    protected fun check(s: String): Boolean {
        skipSpaces()
        if (eof()) {
            return false
        }
        return tokens[current].str() == s
    }

    protected fun checkOffset(offset: Int, expects: String): Boolean {
        skipSpaces()
        if (eof() || eof(offset)) {
            return false
        }
        return tokens[current + offset].str() == expects
    }

    protected inline fun<reified T> check(): Boolean {
        skipSpaces()
        if (eof()) {
            return false
        }
        return tokens[current] is T
    }

    protected inline fun<reified T> rule(fn: () -> T?): T? {
        val saved = current
        val result = fn()
        if (result == null) {
            current = saved
        }
        return result
    }
}