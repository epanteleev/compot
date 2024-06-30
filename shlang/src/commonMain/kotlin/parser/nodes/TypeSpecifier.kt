package parser.nodes

import parser.nodes.visitors.TypeSpecifierVisitor
import types.*


abstract class TypeSpecifier : Node() {
    abstract fun<T> accept(visitor: TypeSpecifierVisitor<T>): T
    abstract fun specifyType(typeHolder: TypeHolder): CType
}

data class DeclarationSpecifier(val specifiers: List<AnyTypeNode>) : TypeSpecifier() {
    internal var isTypedef = false
    private var cachedType: CType? = null

    override fun specifyType(typeHolder: TypeHolder): CType = memoizeType {
        val typeBuilder = CTypeBuilder()
        for (specifier in specifiers) {
            val property = specifier.typeResolve(typeHolder, typeBuilder)
            if (property == StorageClass.TYPEDEF) {
                isTypedef = true
            }
        }
        return@memoizeType typeBuilder.build(typeHolder)
    }

    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    private fun memoizeType(type: () -> CType): CType {
        return cachedType ?: type().also { cachedType = it }
    }
}

data class TypeName(val specifiers: DeclarationSpecifier, val abstractDecl: AbstractDeclarator?) : TypeSpecifier() {
    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    override fun specifyType(typeHolder: TypeHolder): CType {
        val specifierType = specifiers.specifyType(typeHolder)
        if (abstractDecl == null) {
            return specifierType
        }
        return abstractDecl.resolveType(specifierType, typeHolder)
    }
}