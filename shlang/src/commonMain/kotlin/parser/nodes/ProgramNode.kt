package parser.nodes

import tokenizer.Position

class ProgramNode(val nodes: MutableList<Node>) {
    fun begin(): Position {
        if (nodes.isEmpty()) {
            return Position.UNKNOWN
        }
        return nodes.first().begin()
    }
}