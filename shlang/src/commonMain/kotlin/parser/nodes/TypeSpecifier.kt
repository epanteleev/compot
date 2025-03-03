package parser.nodes

import common.assertion
import types.*
import parser.nodes.visitors.TypeSpecifierVisitor
import tokenizer.Position
import typedesc.CTypeBuilder
import typedesc.StorageClass
import typedesc.TypeDesc
import typedesc.TypeHolder
import typedesc.VarDescriptor


sealed class TypeSpecifier {
    private var cachedType: VarDescriptor? = null

    abstract fun begin(): Position
    abstract fun<T> accept(visitor: TypeSpecifierVisitor<T>): T
    abstract fun specifyType(typeHolder: TypeHolder, pointers: List<NodePointer>): VarDescriptor

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

    fun specifyType(typeHolder: TypeHolder) = memoizeType {
        val typeBuilder = CTypeBuilder()
        for (specifier in specifiers) {
            specifier.typeResolve(typeHolder, typeBuilder)
        }

        return@memoizeType typeBuilder.build()
    }

    override fun specifyType(typeHolder: TypeHolder, pointers: List<NodePointer>): VarDescriptor {
        val type = specifyType(typeHolder)
        if (pointers.isEmpty()) {
            return type
        }

        var pointerType = type.typeDesc.cType()
        for (idx in 0 until pointers.size - 1) {
            val pointer = pointers[idx]
            pointerType = CPointer(pointerType, pointer.property())
        }

        return VarDescriptor(TypeDesc.from(CPointer(pointerType), type.typeDesc.qualifiers()), type.storageClass)
    }

    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)
}

data class TypeName(val specifiers: DeclarationSpecifier, val abstractDecl: AbstractDeclarator?) : TypeSpecifier() {
    override fun begin(): Position = specifiers.begin()
    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    fun specifyType(typeHolder: TypeHolder): VarDescriptor {
        val specifierType = specifiers.specifyType(typeHolder)
        if (abstractDecl == null) {
            return specifierType
        }

        return VarDescriptor(abstractDecl.resolveType(specifierType.typeDesc, typeHolder), specifierType.storageClass)
    }

    override fun specifyType(typeHolder: TypeHolder, pointers: List<NodePointer>): VarDescriptor {
        val specifierType = specifiers.specifyType(typeHolder, pointers)
        if (abstractDecl == null) {
            return specifierType
        }

        return VarDescriptor(abstractDecl.resolveType(specifierType.typeDesc, typeHolder), specifierType.storageClass)
    }
}