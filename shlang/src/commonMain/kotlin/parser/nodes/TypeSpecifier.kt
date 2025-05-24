package parser.nodes

import common.assertion
import parser.nodes.visitors.TypeSpecifierVisitor
import sema.SemanticAnalysis
import tokenizer.Position
import sema.CTypeBuilder
import typedesc.TypeHolder
import typedesc.DeclSpec


sealed class TypeSpecifier(private val id: Int) {
    abstract fun begin(): Position
    abstract fun<T> accept(visitor: TypeSpecifierVisitor<T>): T

    final override fun hashCode(): Int = id
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TypeSpecifier

        return id == other.id
    }
}

class DeclarationSpecifier internal constructor(id: Int, val specifiers: List<AnyTypeNode>) : TypeSpecifier(id) {
    init {
        assertion(specifiers.isNotEmpty()) { "DeclarationSpecifier should have at least one specifier" }
    }

    override fun begin(): Position = specifiers.first().begin()

    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)
}

class TypeName internal constructor(id: Int, val specifiers: DeclarationSpecifier, val abstractDeclarator: AbstractDeclarator?) : TypeSpecifier(id) {
    override fun begin(): Position = specifiers.begin()
    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)
}