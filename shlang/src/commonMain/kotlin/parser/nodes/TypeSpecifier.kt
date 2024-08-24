package parser.nodes

import types.*
import parser.nodes.visitors.TypeSpecifierVisitor


sealed class TypeSpecifier : Node() {
    private var cachedType: CType? = null
    private var cachedStorage: StorageClass? = null

    abstract fun<T> accept(visitor: TypeSpecifierVisitor<T>): T
    abstract fun specifyType(typeHolder: TypeHolder, pointers: List<NodePointer>): CType

    fun storageClass(): StorageClass? {
        return cachedStorage
    }

    protected fun memoizeType(type: () -> Pair<CType, StorageClass?>): CType {
        if (cachedType == null) {
            val (resType, storage) = type()
            cachedType = resType
            cachedStorage = storage
        }

        return cachedType!!
    }
}

data class DeclarationSpecifier(val specifiers: List<AnyTypeNode>) : TypeSpecifier() {
    internal var isTypedef = false

    private fun specifyType1(typeHolder: TypeHolder, pointers: List<NodePointer>) = memoizeType {
        val typeBuilder = CTypeBuilder()
        for (specifier in specifiers) {
            val property = specifier.typeResolve(typeHolder, typeBuilder)
            if (property == StorageClass.TYPEDEF) {
                isTypedef = true
            }
        }
        return@memoizeType typeBuilder.build(typeHolder, pointers.isEmpty())
    }

    override fun specifyType(typeHolder: TypeHolder, pointers: List<NodePointer>): CType {
        val type = specifyType1(typeHolder, pointers)

        if (pointers.isEmpty()) {
            return type
        }

        var pointerType = type
        for (idx in 0 until pointers.size - 1) {
            val pointer = pointers[idx]
            pointerType = CPointerType(pointerType, pointer.property())
        }

        val storageClass = storageClass()
        return if (storageClass != null) {
            CPointerType(pointerType, pointers.last().property() + storageClass)
        } else {
            CPointerType(pointerType, pointers.last().property())
        }
    }

    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)
}

data class TypeName(val specifiers: DeclarationSpecifier, val abstractDecl: AbstractDeclarator?) : TypeSpecifier() {
    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    override fun specifyType(typeHolder: TypeHolder, pointers: List<NodePointer>): CType {
        val specifierType = specifiers.specifyType(typeHolder, pointers)
        if (abstractDecl == null) {
            return specifierType
        }
        return abstractDecl.resolveType(specifierType, typeHolder)
    }
}