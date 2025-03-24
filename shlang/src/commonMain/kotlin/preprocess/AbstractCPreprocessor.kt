package preprocess

import parser.*
import preprocess.macros.MacroExpansionException
import tokenizer.*
import tokenizer.tokens.*


data class PreprocessorException(val info: String, val position: Position? = null) : Exception(info) {
    override fun toString(): String {
        if (position == null) {
            return info
        }
        return "PreprocessorException: $info in \"${position.filename()}\" at ${position.line()}:${position.pos()}"
    }
}

sealed class AbstractCPreprocessor(val filename: String, protected val tokens: TokenList) {
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
            throw ParserException(EndOfFile(filename))
        }
        if (current !is T) {
            throw ParserException(InvalidToken("Expected ${T::class}, but got: ", current!!))
        }
        return current as T
    }

    protected fun check(s: String): Boolean {
        if (eof()) {
            return false
        }
        return current!!.str() == s
    }

    protected fun checkNextMacro(vararg expected: String): Boolean {
        if (eof()) {
            return false
        }
        var next = current!!.next()
        while (next is Indent) {
            next = next.next()
        }

        for (s in expected) {
            if (next != null && next.str() == s) {
                return true
            }
        }
        return false
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
        val result = current!!
        current = tokens.kill(current!!)
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

    protected fun killNewLines(): Int {
        var removed = 0
        while (!eof() && check<NewLine>()) {
            val indent = kill()
            removed += indent.str().length
        }
        return removed
    }

    protected fun eatSpaces() {
        do {
            if (eof()) {
                throw MacroExpansionException("Invalid macro expansion")
            }

            if (current is CToken) {
                return
            }
            eat()
        } while (true)
    }

    protected fun addAll(others: TokenList) {
        if (others.isEmpty()) {
            return
        }
        val first = others.first()
        if (current == null) {
            tokens.addAll(others)
        } else {
            tokens.addAll(current!!, others)
        }
        current = first
    }

    protected fun addInclude(tokens: TokenList) {
        if (tokens.isEmpty()) {
            return
        }
        val last = tokens.last()
        if (current == null) {
            this.tokens.addAll(tokens)
        } else {
            this.tokens.addAll(current!!, tokens)
        }
        current = last
    }

    protected fun add(tok: AnyToken) {
        if (current != null) {
            tokens.addBefore(current!!, tok)
            return
        }

        tokens.add(tok)
        current = tok
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

    protected fun warning(message: String, position: Position) {
        println("Warning: $message in $position")
    }
}