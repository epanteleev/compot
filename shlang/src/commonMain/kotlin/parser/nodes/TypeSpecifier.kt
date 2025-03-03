package parser.nodes

import common.assertion
import parser.nodes.visitors.TypeSpecifierVisitor
import tokenizer.Position
import typedesc.CTypeBuilder
import typedesc.TypeHolder
import typedesc.VarDescriptor


sealed class TypeSpecifier {
    private var cachedType: VarDescriptor? = null

    abstract fun begin(): Position
    abstract fun<T> accept(visitor: TypeSpecifierVisitor<T>): T
    abstract fun specifyType(typeHolder: TypeHolder): VarDescriptor

    protected fun memoizeType(type: () -> VarDescriptor): VarDescriptor {
        if (cachedType == null) {
            cachedType = type()
        }

        return cachedType!!
    }
}

data class DeclarationSpecifier(val specifiers: List<AnyTypeNode>) : TypeSpecifier() {
    init {
        assertion(specifiers.isNotEmpty()) { "DeclarationSpecifier should have at least one specifier" }
    }

    override fun begin(): Position = specifiers.first().begin()

    override fun specifyType(typeHolder: TypeHolder) = memoizeType {
        val typeBuilder = CTypeBuilder()
        for (specifier in specifiers) {
            specifier.typeResolve(typeHolder, typeBuilder)
        }

        return@memoizeType typeBuilder.build()
    }

    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)
}

data class TypeName(val specifiers: DeclarationSpecifier, val abstractDeclarator: AbstractDeclarator?) : TypeSpecifier() {
    override fun begin(): Position = specifiers.begin()
    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    override fun specifyType(typeHolder: TypeHolder): VarDescriptor {
        val specifierType = specifiers.specifyType(typeHolder)
        if (abstractDeclarator == null) {
            return specifierType
        }

        return VarDescriptor(abstractDeclarator.resolveType(specifierType.typeDesc, typeHolder), specifierType.storageClass)
    }
}