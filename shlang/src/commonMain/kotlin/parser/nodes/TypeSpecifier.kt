package parser.nodes

import parser.nodes.visitors.TypeSpecifierVisitor
import types.*


abstract class TypeSpecifier : Node() {
    abstract fun<T> accept(visitor: TypeSpecifierVisitor<T>): T
    abstract fun resolveType(typeHolder: TypeHolder): CType
}

data class DeclarationSpecifier(val specifiers: List<AnyTypeNode>) : TypeSpecifier() {
    var isTypedef = false

    override fun resolveType(typeHolder: TypeHolder): CType {
        val typeBuilder = CTypeBuilder()
        for (specifier in specifiers) {
            val property = specifier.typeResolve(typeHolder, typeBuilder)
            if (property == StorageClass.TYPEDEF) {
                isTypedef = true
            }
        }
        return typeBuilder.build(typeHolder)
    }

    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    companion object {
        val EMPTY = DeclarationSpecifier(emptyList())
    }
}

data class TypeName(val specifiers: DeclarationSpecifier, val abstractDecl: AbstractDeclarator?) : TypeSpecifier() {
    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        val specifierType = specifiers.resolveType(typeHolder)
        if (abstractDecl == null) {
            return specifierType
        }
        return abstractDecl.resolveType(specifierType, typeHolder)
    }
}