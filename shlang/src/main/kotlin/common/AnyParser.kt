package common

import tokenizer.Eof
import tokenizer.CToken
import parser.ProgramMessage
import parser.ParserException
import tokenizer.AnyToken
import tokenizer.Indent


abstract class AnyParser(protected val tokens: MutableList<AnyToken>) {
    protected var current: Int = 0

    protected fun eat() {
        if (eof()) {
            throw ParserException(ProgramMessage("Unexpected EOF", peak()))
        }
        current += 1
    }

    protected fun eof(): Boolean {
        return eof(0)
    }

    protected fun eof(offset: Int): Boolean {
        return current + offset >= tokens.size || tokens[current + offset] is Eof
    }

    protected inline fun<reified T: AnyToken> peak(): T {
        return peak(0)
    }

    protected inline fun<reified T: AnyToken> peak(offset: Int): T {
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