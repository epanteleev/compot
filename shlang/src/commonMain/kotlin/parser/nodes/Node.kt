package parser.nodes

import parser.nodes.visitors.*
import tokenizer.Position


sealed class Node {
    fun<T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }

    abstract fun begin(): Position
}