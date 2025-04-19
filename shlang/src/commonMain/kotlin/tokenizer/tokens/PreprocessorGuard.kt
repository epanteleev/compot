package tokenizer.tokens

import tokenizer.Position

sealed class PreprocessorGuard(val filename: String, val includeLevel: Int, val line: Int): AnyToken()

class EnterIncludeGuard(filename: String, includeLevel: Int, line: Int): PreprocessorGuard(filename, includeLevel, line) {
    override fun str(): String = "#enter[$includeLevel] $filename in $line\n"

    override fun cloneWith(pos: Position): AnyToken {
        return EnterIncludeGuard(filename, includeLevel, line)
    }

    override fun hashCode(): Int {
        return filename.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EnterIncludeGuard

        return filename == other.filename && includeLevel == other.includeLevel && line == other.line
    }
}

class ExitIncludeGuard(filename: String, includeLevel: Int, line: Int): PreprocessorGuard(filename, includeLevel, line) {
    override fun str(): String = "#exit[$includeLevel] $filename in $line\n"

    override fun cloneWith(pos: Position): AnyToken {
        return ExitIncludeGuard(filename, includeLevel, line)
    }

    override fun hashCode(): Int {
        return filename.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExitIncludeGuard

        return filename == other.filename && includeLevel == other.includeLevel && line == other.line
    }
}