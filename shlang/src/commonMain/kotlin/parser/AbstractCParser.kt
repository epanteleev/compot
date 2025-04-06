package parser

import tokenizer.*
import typedesc.TypeHolder
import tokenizer.tokens.*

class FunctionCtx(val labelResolver: LabelResolver, val typeHolder: TypeHolder)


sealed class AbstractCParser(val filename: String, tokens: TokenList) {
    private var anonymousCounter = 0
    protected var current: AnyToken? = tokens.firstOrNull()
    protected val globalTypeHolder = TypeHolder.default()
    protected var funcCtx: FunctionCtx? = FunctionCtx(LabelResolver.default(), globalTypeHolder)

    fun globalTypeHolder(): TypeHolder = globalTypeHolder

    protected fun typeHolder(): TypeHolder = funcCtx?.typeHolder ?: globalTypeHolder

    protected fun anonymousName(prefix: String): String {
        return "$prefix.${anonymousCounter++}"
    }

    protected fun eof(): Boolean {
        skipSpaces()
        return current == null
    }

    protected fun skipSpaces() {
        while (current != null && current !is CToken) {
            current = current!!.next()
        }
    }

    protected fun eat(): CToken {
        skipSpaces()
        if (eof()) {
            throw ParserException(EndOfFile(filename))
        }
        val token = current as CToken
        current = current!!.next()
        return token
    }

    protected inline fun <reified T : CToken> peak(): T {
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

    protected inline fun<reified T> funcRule(fn: () -> T?): T? {
        funcCtx = FunctionCtx(LabelResolver.default(), globalTypeHolder.copy())
        val result = rule(fn)
        funcCtx!!.labelResolver.resolveAll()
        funcCtx = null
        return result
    }
}