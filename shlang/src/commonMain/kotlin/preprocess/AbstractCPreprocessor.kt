package preprocess

import parser.*
import tokenizer.*


data class PreprocessorException(val info: String, val position: Position? = null) : Exception(info) {
    override fun toString(): String {
        if (position == null) {
            return info
        }
        return "PreprocessorException: $info in \"${position.filename()}\" at ${position.line()}:${position.pos()}"
    }
}


abstract class AbstractCPreprocessor(protected val tokens: TokenList) {
    protected var current: AnyToken? = tokens.firstOrNull()

    protected fun eof(): Boolean {
        return current == null
    }

    protected fun eat() {
        if (eof()) {
            throw PreprocessorException("Unexpected EOF")
        }
        current = current!!.next()
    }

    protected inline fun<reified T: AnyToken> peak(): T {
        if (eof()) {
            throw ParserException(EndOfFile)
        }
        if (current !is T) {
            throw ParserException(InvalidToken("Unexpected token $current", current!!))
        }
        return current as T
    }

    protected fun check(s: String): Boolean {
        if (eof()) {
            return false
        }
        return current!!.str() == s
    }

    protected inline fun<reified T> check(): Boolean {
        if (eof()) {
            return false
        }
        return current is T
    }

    protected fun kill(): AnyToken {
        if (eof()) {
            throw PreprocessorException("Unexpected EOF")
        }
        val next = current!!.next()
        val result = tokens.remove(current!!)
        current = next
        return result
    }


    protected fun killWithSpaces(): AnyToken {
        val tok = kill()
        killSpaces()

        return tok
    }

    protected fun killSpaces(): Int {
        var removed = 0
        while (!eof() && check<Indent>()) {
            val indent = kill()
            removed += indent.str().length
        }
        return removed
    }

    protected fun addAll(others: TokenList) {
        if (others.isEmpty()) {
            return
        }
        val first = others.firstOrNull()
        if (current == null) {
            tokens.addAll(others)
        } else {
            tokens.addAll(current!!, others)
        }
        current = first
    }

    protected fun add(tok: AnyToken) {
        if (current == null) {
            tokens.add(tok)
            current = tok
            return
        }
        tokens.addBefore(current!!, tok)
    }

    protected fun trimSpacesAtEnding() {
        if (tokens.isEmpty()) {
            return
        }
        while (tokens.last() is AnySpaceToken) {
            tokens.removeLast()
            if (tokens.isEmpty()) {
                return
            }
        }
    }
}