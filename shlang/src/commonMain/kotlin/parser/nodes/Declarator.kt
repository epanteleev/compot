package parser.nodes

import types.*
import parser.nodes.visitors.*
import tokenizer.Position
import typedesc.StorageClass
import typedesc.TypeDesc
import typedesc.TypeHolder
import typedesc.TypeResolutionException
import typedesc.VarDescriptor


sealed class AnyDeclarator {
    private var cachedType: VarDescriptor? = null

    abstract fun begin(): Position
    abstract fun name(): String
    abstract fun<T> accept(visitor: DeclaratorVisitor<T>): T
    internal abstract fun declareType(varDesc: VarDescriptor, typeHolder: TypeHolder): VarDescriptor

    protected fun wrapPointers(type: CType, pointers: List<NodePointer>): CType {
        var pointerType = type
        for (idx in pointers.indices) {
            val pointer = pointers[idx]
            pointerType = CPointer(pointerType, pointer.property())
        }
        return pointerType
    }

    protected fun memoizeType(type: () -> VarDescriptor): VarDescriptor {
        if (cachedType == null) {
            cachedType = type()
        }

        return cachedType!!
    }
}

data class Declarator(val directDeclarator: DirectDeclarator, val pointers: List<NodePointer>): AnyDeclarator() {
    override fun begin(): Position = directDeclarator.begin()
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return directDeclarator.name()
    }

    override fun declareType(varDesc: VarDescriptor, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        var pointerType = wrapPointers(varDesc.typeDesc.cType(), pointers)
        val newTypeDesc = TypeDesc.from(pointerType, varDesc.typeDesc.qualifiers())
        val type = directDeclarator.resolveType(newTypeDesc, typeHolder)

        if (varDesc.storageClass == StorageClass.TYPEDEF) {
            typeHolder.addTypedef(name(), type)
            return@memoizeType VarDescriptor(type, varDesc.storageClass)
        }

        val varDesc = VarDescriptor(type, varDesc.storageClass)
        val baseType = type.cType()
        if (baseType is CFunctionType) {
            // declare extern function or function without body
            typeHolder.addFunctionType(name(), varDesc)
        } else {
            typeHolder.addVar(name(), varDesc)
        }
        return@memoizeType varDesc
    }
}

data class InitDeclarator(val declarator: Declarator, val rvalue: Initializer): AnyDeclarator() {
    override fun begin(): Position = declarator.begin()
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return declarator.name()
    }

    override fun declareType(varDesc: VarDescriptor, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        var pointerType = wrapPointers(varDesc.typeDesc.cType(), declarator.pointers)
        val newTypeDesc = TypeDesc.from(pointerType, varDesc.typeDesc.qualifiers())

        val type = declarator.directDeclarator.resolveType(newTypeDesc, typeHolder)
        val baseType = type.cType()
        if (baseType !is CUncompletedArrayType) {
            return@memoizeType typeHolder.addVar(name(), VarDescriptor(type, varDesc.storageClass))
        }

        when (rvalue) {
            is InitializerListInitializer -> {
                // Special case for array initialization without exact size like:
                // int a[] = {1, 2};
                // 'a' is array of 2 elements, not pointer to int

                val initializerList = rvalue.list
                when (val initializerType = initializerList.resolveType(typeHolder)) {
                    is InitializerType -> {
                        val rvalueType = TypeDesc.from(CArrayType(baseType.element(), initializerList.length().toLong()), listOf())
                        return@memoizeType typeHolder.addVar(name(), VarDescriptor(rvalueType, varDesc.storageClass))
                    }
                    is CStringLiteral -> {
                        val rvalueType = TypeDesc.from(CArrayType(baseType.element(), initializerType.dimension + 1), listOf())
                        return@memoizeType typeHolder.addVar(name(), VarDescriptor(rvalueType, varDesc.storageClass))
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
                return@memoizeType typeHolder.addVar(name(), VarDescriptor(TypeDesc.from(expr.resolveType(typeHolder)), varDesc.storageClass))
            }
        }
    }
}