package common

import tokenizer.Eof
import tokenizer.CToken
import parser.ProgramMessage
import parser.ParserException


abstract class AnyParser(protected val tokens: List<CToken>) {
    protected var current: Int = 0

    protected fun eat() {
        if (eof()) {
            throw ParserException(ProgramMessage("Unexpected EOF", peak()))
        }
        current += 1
    }

    protected fun eof(): Boolean {
        return tokens[current] is Eof
    }

    protected fun eof(offset: Int): Boolean {
        return current + offset >= tokens.size || tokens[current + offset] is Eof
    }

    protected inline fun<reified T: CToken> peak(): T {
        if (eof()) {
            throw ParserException(ProgramMessage("Unexpected EOF", tokens[current]))
        }
        return tokens[current] as T
    }

    protected inline fun<reified T: CToken> peak(offset: Int): T {
        if (eof(offset)) {
            throw ParserException(ProgramMessage("Unexpected EOF", tokens[current]))
        }
        return tokens[current + offset] as T
    }

    protected fun check(s: String): Boolean {
        if (eof()) {
            return false
        }
        return tokens[current].str() == s
    }

    protected fun checkOffset(offset: Int, expects: String): Boolean {
        if (eof() || eof(offset)) {
            return false
        }
        return tokens[current + offset].str() == expects
    }

    protected inline fun<reified T> check(): Boolean {
        if (eof()) {
            return false
        }
        return tokens[current] is T
    }
}