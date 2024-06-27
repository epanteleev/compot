package tokenizer

interface Position {
    fun line(): Int
    fun pos(): Int
    fun filename(): String

    companion object {
        val UNKNOWN = OriginalPosition(-1, -1, "<unknown>")
    }
}


data class OriginalPosition(private val line: Int, private val pos: Int, private val filename: String = "<unknown>") : Position {
    override fun toString(): String {
        return "[$line:$pos]"
    }

    override fun line(): Int = line
    override fun pos(): Int = pos
    override fun filename(): String = filename

    companion object {
        val UNKNOWN = OriginalPosition(-1, -1, "<unknown>")
    }
}

data class PreprocessedPosition(private val line: Int, private val pos: Int, private val filename: String = "<unknown>", val origin: OriginalPosition) : Position {
    override fun toString(): String {
        return "[$line:$pos]"
    }

    override fun line(): Int = line
    override fun pos(): Int = pos
    override fun filename(): String = filename

    companion object {
        val UNKNOWN = PreprocessedPosition(-1, -1, "<unknown>", OriginalPosition.UNKNOWN)
        fun makeFrom(macrosPos: Position, origin: OriginalPosition): PreprocessedPosition {
            return PreprocessedPosition(macrosPos.line(), macrosPos.pos(), macrosPos.filename(), origin)
        }
    }
}