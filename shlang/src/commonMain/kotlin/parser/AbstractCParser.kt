package parser

import tokenizer.*
import types.TypeHolder


abstract class AbstractCParser(val filename: String, tokens: TokenList) {
    protected var current: AnyToken? = tokens.firstOrNull()
    protected val typeHolder = TypeHolder.default()

    fun typeHolder(): TypeHolder = typeHolder

    protected fun eof(): Boolean {
        skipSpaces()
        return current == null
    }

    protected fun skipSpaces() {
        while (current != null && current !is CToken) {
            current = current!!.next()
        }
    }

    protected fun eat() {
        skipSpaces()
        if (eof()) {
            throw ParserException(EndOfFile(filename))
        }
        current = current!!.next()
    }

    protected inline fun <reified T : AnyToken> peak(): T {
        skipSpaces()
        if (eof()) {
            throw ParserException(EndOfFile(filename))
        }
        if (current !is T) {
            throw ParserException(InvalidToken("Unexpected token $current", current!!))
        }
        return current as T
    }

    protected fun check(s: String): Boolean {
        skipSpaces()
        if (eof()) {
            return false
        }
        return current!!.str() == s
    }

    protected fun checkOffset(offset: Int, expects: String): Boolean {
        skipSpaces()
        var iter = 0
        var current = current
        do {
            if (eof()) {
                return false
            }
            if (iter == offset) {
                return current!!.str() == expects
            }

            current = current!!.next()
            iter++
        } while (true)
    }

    protected inline fun<reified T> check(): Boolean {
        skipSpaces()
        if (eof()) {
            return false
        }
        return current is T
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