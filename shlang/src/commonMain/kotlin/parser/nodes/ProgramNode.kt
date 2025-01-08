package parser.nodes

import tokenizer.OriginalPosition
import tokenizer.Position

class ProgramNode(val filename: String, val nodes: MutableList<Node>) {
    fun begin(): Position {
        if (nodes.isEmpty()) {
            return OriginalPosition(0, 0, filename)
        }

        return nodes.first().begin()
    }
}