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


sealed class TypeSpecifier : Node() {
    private var cachedType: VarDescriptor? = null

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
    internal var isTypedef = false

    init {
        assertion(specifiers.isNotEmpty()) { "DeclarationSpecifier should have at least one specifier" }
    }

    override fun begin(): Position = specifiers.first().begin()

    private fun specifyType1(typeHolder: TypeHolder) = memoizeType {
        val typeBuilder = CTypeBuilder()
        for (specifier in specifiers) {
            val property = specifier.typeResolve(typeHolder, typeBuilder)
            if (property == StorageClass.TYPEDEF) {
                isTypedef = true
            }
        }

        return@memoizeType typeBuilder.build()
    }

    override fun specifyType(typeHolder: TypeHolder, pointers: List<NodePointer>): VarDescriptor {
        val type = specifyType1(typeHolder)
        if (pointers.isEmpty()) {
            return type
        }

        var pointerType = type.type.cType()
        for (idx in 0 until pointers.size - 1) {
            val pointer = pointers[idx]
            pointerType = CPointer(pointerType, pointer.property().toSet())
        }

        return VarDescriptor(TypeDesc.from(CPointer(pointerType), type.type.qualifiers()), type.storageClass)
    }

    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)
}

data class TypeName(val specifiers: DeclarationSpecifier, val abstractDecl: AbstractDeclarator?) : TypeSpecifier() {
    override fun begin(): Position = specifiers.begin()
    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    override fun specifyType(typeHolder: TypeHolder, pointers: List<NodePointer>): VarDescriptor {
        val specifierType = specifiers.specifyType(typeHolder, pointers)
        if (abstractDecl == null) {
            return specifierType
        }

        return VarDescriptor(abstractDecl.resolveType(specifierType.type, typeHolder), specifierType.storageClass)
    }
}

data class Declaration(val declspec: DeclarationSpecifier, private val declarators: List<AnyDeclarator>): TypeSpecifier() {
    override fun begin(): Position = declspec.begin()

    fun nonTypedefDeclarators(): List<AnyDeclarator> {
        if (declspec.isTypedef) {
            return listOf()
        }

        return declarators
    }

    fun declarators(): List<AnyDeclarator> {
        return declarators
    }

    override fun <T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    override fun specifyType(typeHolder: TypeHolder, pointers: List<NodePointer>): VarDescriptor {
        for (it in declarators) {
            it.declareType(declspec, typeHolder)
        }

        return declspec.specifyType(typeHolder, pointers)
    }
}