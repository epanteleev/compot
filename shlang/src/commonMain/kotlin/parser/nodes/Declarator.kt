package parser.nodes

import types.*
import parser.nodes.visitors.*
import tokenizer.Position
import typedesc.StorageClass
import typedesc.TypeDesc
import typedesc.TypeHolder
import typedesc.TypeResolutionException
import typedesc.DeclSpec
import typedesc.VarDescriptor


sealed class AnyDeclarator {
    abstract fun begin(): Position
    abstract fun name(): String
    abstract fun<T> accept(visitor: DeclaratorVisitor<T>): T
    internal abstract fun declareVar(declSpec: DeclSpec, typeHolder: TypeHolder): VarDescriptor?

    protected fun wrapPointers(type: CType, pointers: List<NodePointer>): CType {
        var pointerType = type
        for (pointer in pointers) {
            pointerType = CPointer(pointerType, pointer.property())
        }
        return pointerType
    }
}

class Declarator(val directDeclarator: DirectDeclarator, val pointers: List<NodePointer>): AnyDeclarator() {
    override fun begin(): Position = directDeclarator.begin()
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return directDeclarator.name()
    }

    fun resolveTypedef(declSpec: DeclSpec, typeHolder: TypeHolder) {
        if (declSpec.storageClass != StorageClass.TYPEDEF) {
            return
        }

        val pointerType = wrapPointers(declSpec.typeDesc.cType(), pointers)
        val newTypeDesc = TypeDesc.from(pointerType, declSpec.typeDesc.qualifiers())

        val type = directDeclarator.resolveType(newTypeDesc, typeHolder)
        typeHolder.addTypedef(name(), type)
    }

    override fun declareVar(declSpec: DeclSpec, typeHolder: TypeHolder): VarDescriptor? {
        if (declSpec.storageClass == StorageClass.TYPEDEF) {
            return null
        }

        val pointerType = wrapPointers(declSpec.typeDesc.cType(), pointers)
        val newTypeDesc = TypeDesc.from(pointerType, declSpec.typeDesc.qualifiers())
        val type = directDeclarator.resolveType(newTypeDesc, typeHolder)
        return VarDescriptor(name(), type.asType(begin()), type.qualifiers(), declSpec.storageClass)
    }
}

class InitDeclarator(val declarator: Declarator, val rvalue: Initializer): AnyDeclarator() {
    override fun begin(): Position = declarator.begin()
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return declarator.name()
    }

    override fun declareVar(declSpec: DeclSpec, typeHolder: TypeHolder): VarDescriptor {
        val pointerType = wrapPointers(declSpec.typeDesc.cType(), declarator.pointers)
        val newTypeDesc = TypeDesc.from(pointerType, declSpec.typeDesc.qualifiers())

        val type = declarator.directDeclarator.resolveType(newTypeDesc, typeHolder)
        val baseType = type.cType()
        if (baseType !is CUncompletedArrayType) {
            return VarDescriptor(name(), baseType.asType(begin()), type.qualifiers(), declSpec.storageClass)
        }

        when (rvalue) {
            is InitializerListInitializer -> {
                // Special case for array initialization without exact size like:
                // int a[] = {1, 2};
                // 'a' is array of 2 elements, not pointer to int

                val initializerList = rvalue.list
                when (val initializerType = initializerList.resolveType(typeHolder)) {
                    is InitializerType -> {
                        val rvalueType = CArrayType(baseType.element(), initializerList.length().toLong())
                        return VarDescriptor(name(), rvalueType, listOf(), declSpec.storageClass)
                    }
                    is CStringLiteral -> {
                        val rvalueType = CArrayType(baseType.element(), initializerType.dimension + 1)
                        return VarDescriptor(name(), rvalueType, listOf(), declSpec.storageClass)
                    }
                    else -> throw TypeResolutionException("Array size is not specified: type=$initializerType", declarator.begin())
                }
            }
            is ExpressionInitializer -> {
                val expr = rvalue.expr
                if (expr !is StringNode) {
                    throw TypeResolutionException("Array size is not specified", declarator.begin())
                }
                // Special case for string initialization like:
                // char a[] = "hello";
                return VarDescriptor(name(), expr.resolveType(typeHolder), listOf(), declSpec.storageClass)
            }
        }
    }
}