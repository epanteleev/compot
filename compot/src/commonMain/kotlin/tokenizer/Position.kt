package tokenizer

sealed class Position(protected val line: Int, protected val pos: Int, protected val filename: String) {
    fun line(): Int = line
    fun pos(): Int = pos
    fun filename(): String = filename

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Position

        if (line != other.line) return false
        if (pos != other.pos) return false
        if (filename != other.filename) return false

        return true
    }

    final override fun hashCode(): Int {
        return 31 * (31 * line + pos) + filename.hashCode()
    }

    companion object {
        val UNKNOWN = OriginalPosition(-1, -1, "<unknown>")
    }
}

class OriginalPosition(line: Int, pos: Int, filename: String) : Position(line, pos, filename) {
    override fun toString(): String {
        return "$filename in [$line:$pos]"
    }
}

class PreprocessedPosition(line: Int, pos: Int, filename: String, private val origin: OriginalPosition) : Position(line, pos, filename) {
    override fun toString(): String {
        return "$filename in [$line:$pos] originated from $origin"
    }

    companion object {
        fun makeFrom(macrosPos: Position, origin: OriginalPosition): PreprocessedPosition {
            return PreprocessedPosition(macrosPos.line(), macrosPos.pos(), macrosPos.filename(), origin)
        }
    }
}