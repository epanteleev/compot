package preprocess

import common.AnyParser
import tokenizer.AnyToken
import parser.*
import tokenizer.Eof
import tokenizer.Indent


data class PreprocessorException(val info: String) : Exception(info)


abstract class AbstractCPreprocessor(tokens: MutableList<AnyToken>): AnyParser(tokens) {
    protected fun eof(): Boolean {
        return eof(0)
    }

    protected fun eof(offset: Int): Boolean {
        return current + offset >= tokens.size || tokens[current + offset] is Eof
    }

    protected fun eat() {
        if (eof()) {
            throw PreprocessorException("Unexpected EOF at ${tokens[current]}")
        }
        current += 1
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

    protected fun kill(): AnyToken = killAt(current)

    protected fun killAt(index: Int): AnyToken = tokens.removeAt(index)

    protected fun killWithSpaces() {
        kill()
        while (!eof() && check<Indent>()) {
            kill()
        }
    }

    protected fun addAll(others: List<AnyToken>) {
        tokens.addAll(current, others)
    }

    protected fun add(tok: AnyToken) {
        tokens.add(current, tok)
    }
}