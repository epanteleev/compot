package parser.nodes

import tokenizer.Identifier
import parser.nodes.visitors.UnclassifiedNodeVisitor


class Designation(val designators: List<Designator>): UnclassifiedNode() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

sealed class Designator: UnclassifiedNode()

class ArrayDesignator(val constExpression: Expression): Designator() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class MemberDesignator(val name: Identifier): Designator() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T {
        return visitor.visit(this)
    }
}