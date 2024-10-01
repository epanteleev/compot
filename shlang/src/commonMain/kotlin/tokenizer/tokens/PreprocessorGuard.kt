package tokenizer.tokens

import tokenizer.Position

sealed class PreprocessorGuard(val filename: String, val includeLevel: Int, val line: Int): AnyToken()

class EnterIncludeGuard(filename: String, includeLevel: Int, line: Int): PreprocessorGuard(filename, includeLevel, line) {
    override fun str(): String = "#enter[$includeLevel] $filename in $line\n"

    override fun cloneWith(pos: Position): AnyToken {
        return this
    }
}

class ExitIncludeGuard(filename: String, includeLevel: Int, line: Int): PreprocessorGuard(filename, includeLevel, line) {
    override fun str(): String = "#exit[$includeLevel] $filename in $line\n"

    override fun cloneWith(pos: Position): AnyToken {
        return this
    }
}