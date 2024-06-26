package preprocess

import tokenizer.AnyToken
import parser.*
import tokenizer.AnySpaceToken
import tokenizer.Indent


data class PreprocessorException(val info: String) : Exception(info)


abstract class AbstractCPreprocessor(protected val tokens: MutableList<AnyToken>) {
    protected var current: Int = 0

    protected fun eof(): Boolean {
        return eof(0)
    }

    protected fun eof(offset: Int): Boolean {
        return current + offset >= tokens.size
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
            throw ParserException(InvalidToken("Unexpected EOF", tokens[current]))
        }
        val tok = tokens[current + offset]
        if (tok !is T) {
            throw ParserException(InvalidToken("Unexpected token $tok", tok))
        }
        return tok
    }

    protected fun check(s: String): Boolean {
        if (eof()) {
            return false
        }
        return tokens[current].str() == s
    }

    protected inline fun<reified T> check(): Boolean {
        if (eof()) {
            return false
        }
        return tokens[current] is T
    }

    protected fun kill(): AnyToken = killAt(current)

    private fun killAt(index: Int): AnyToken = tokens.removeAt(index)

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

    protected fun trimSpacesAtEnding() {
        if (tokens.isEmpty()) {
            return
        }
        while (tokens.last() is AnySpaceToken) {
            tokens.removeLast()
        }
    }
}