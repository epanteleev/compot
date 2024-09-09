package parser.nodes

import types.*
import common.assertion
import parser.nodes.visitors.*


sealed class AnyDeclarator: Node() {
    protected var cachedType: VarDescriptor? = null

    abstract fun name(): String
    abstract fun<T> accept(visitor: DeclaratorVisitor<T>): T
    internal abstract fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor

    fun cType(): VarDescriptor {
        if (cachedType == null) {
            throw IllegalStateException("type is not resolved")
        }

        return cachedType!!
    }

    protected fun memoizeType(type: () -> VarDescriptor): VarDescriptor {
        if (cachedType == null) {
            cachedType = type()
        }

        return cachedType!!
    }
}

data class Declarator(val directDeclarator: DirectDeclarator, val pointers: List<NodePointer>): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return directDeclarator.name()
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        val declspecType = declspec.specifyType(typeHolder, pointers)
        val type = directDeclarator.resolveType(declspecType.type, typeHolder)
        if (declspec.isTypedef) {
            assertion(declspecType.storageClass == null) { "typedef with storage class is not supported" }

            typeHolder.addNewType(name(), TypeDef(name(), type))
            typeHolder.addTypedef(name(), type)
            return@memoizeType VarDescriptor(type, declspecType.storageClass)
        }

        val varDesc = VarDescriptor(type, declspecType.storageClass)
        if (type is CFunctionType) {
            // declare extern function or function without body
            typeHolder.addFunctionType(name(), varDesc)
        } else {
            typeHolder.addVar(name(), varDesc)
        }
        return@memoizeType varDesc
    }
}

data class InitDeclarator(val declarator: Declarator, val rvalue: Expression): AnyDeclarator() { //TODO rename
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return declarator.name()
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        val declspecType = declspec.specifyType(typeHolder, declarator.pointers)

        val type = declarator.directDeclarator.resolveType(declspecType.type, typeHolder)
        assertion (!declspec.isTypedef) { "typedef is not supported here" }

        if (type !is UncompletedArrayType) {
            return@memoizeType typeHolder.addVar(name(), VarDescriptor(type, declspecType.storageClass))
        }

        when (rvalue) {
            is InitializerList -> {
                // Special case for array initialization without exact size like:
                // int a[] = {1, 2};
                // 'a' is array of 2 elements, not pointer to int

                val rvalueType = CArrayType(CArrayBaseType(type.element(), rvalue.length().toLong()), listOf())
                return@memoizeType typeHolder.addVar(name(), VarDescriptor(rvalueType, declspecType.storageClass))
            }
            is StringNode -> {
                // Special case for string initialization like:
                // char a[] = "hello";
                val rvalueType = rvalue.resolveType(typeHolder)
                return@memoizeType typeHolder.addVar(name(), VarDescriptor(rvalueType, declspecType.storageClass))
            }
            else -> throw TypeResolutionException("Array size is not specified")
        }
    }
}

data object EmptyDeclarator : AnyDeclarator() {
    override fun name(): String = ""

    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor {
        throw TypeResolutionException("Empty declarator is not supported")
    }
}

data class StructDeclarator(val declarator: AnyDeclarator, val expr: Expression): AnyDeclarator() {
    override fun <T> accept(visitor: DeclaratorVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        require(expr is EmptyExpression) {
            "unsupported expression in struct declarator $expr"
        }

        return@memoizeType declarator.declareType(declspec, typeHolder)
    }

    override fun name(): String = declarator.name()
}

data class FunctionNode(val specifier: DeclarationSpecifier,
                        val declarator: Declarator,
                        val body: Statement) : AnyDeclarator() {
    override fun name(): String {
        return declarator.directDeclarator.decl.name()
    }

    fun functionDeclarator(): ParameterTypeList {
        return declarator.directDeclarator.directDeclaratorParams[0] as ParameterTypeList
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        assertion(specifier === declspec) { "specifier should be the same" }

        val declspecType = declspec.specifyType(typeHolder, declarator.pointers)

        val type = declarator.directDeclarator.resolveType(declspecType.type, typeHolder)
        assertion(!declspec.isTypedef) { "typedef is not supported here" }

        assertion(type is CFunctionType) { "function type expected" }
        return@memoizeType typeHolder.addFunctionType(name(), VarDescriptor(type, declspecType.storageClass))
    }

    fun resolveType(typeHolder: TypeHolder): CFunctionType {
        return declareType(specifier, typeHolder).type as CFunctionType
    }

    override fun <T> accept(visitor: DeclaratorVisitor<T>): T = visitor.visit(this)
}