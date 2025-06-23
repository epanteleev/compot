package parser.nodes

import parser.nodes.visitors.*
import tokenizer.Position


sealed class AnyDeclarator(private val id: Int) {
    abstract fun begin(): Position
    abstract fun name(): String
    abstract fun<T> accept(visitor: DeclaratorVisitor<T>): T

    final override fun hashCode(): Int {
        return id
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AnyDeclarator

        return id == other.id
    }
}

class Declarator internal constructor(id: Int, val directDeclarator: DirectDeclarator, val pointers: List<NodePointer>): AnyDeclarator(id) {
    override fun begin(): Position = directDeclarator.begin()
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return directDeclarator.name()
    }
}

class InitDeclarator internal constructor(id: Int, val declarator: Declarator, val rvalue: Initializer): AnyDeclarator(id) {
    override fun begin(): Position = declarator.begin()
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return declarator.name()
    }
}