package parser.nodes

import types.*
import common.assertion
import parser.nodes.visitors.*


sealed class AnyDeclarator: Node() {
    protected var cachedType: TypeDesc = TypeDesc.UNRESOlVED

    abstract fun name(): String
    abstract fun<T> accept(visitor: DeclaratorVisitor<T>): T
    internal abstract fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): TypeDesc

    fun cType(): TypeDesc {
        if (cachedType == TypeDesc.UNRESOlVED) {
            throw IllegalStateException("type is not resolved")
        }

        return cachedType
    }

    protected inline fun<reified T: TypeDesc> memoizeType(type: () -> T): T {
        if (cachedType == TypeDesc.UNRESOlVED) {
            cachedType = type()
        }

        return cachedType as T
    }
}

data class Declarator(val directDeclarator: DirectDeclarator, val pointers: List<NodePointer>): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return directDeclarator.name()
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): TypeDesc = memoizeType {
        var pointerType = declspec.specifyType(typeHolder, pointers)
        pointerType = directDeclarator.resolveType(pointerType, declspec.storageClass(), typeHolder)
        if (declspec.isTypedef) {
            typeHolder.addNewType(name(), TypeDef(name(), pointerType))
            typeHolder.addTypedef(name(), pointerType)
            return@memoizeType pointerType
        }

        if (pointerType is CFunctionType) {
            // declare extern function or function without body
            typeHolder.addFunctionType(name(), pointerType)
        } else {
            typeHolder.addVar(name(), pointerType)
        }
        return@memoizeType pointerType
    }
}

data class InitDeclarator(val declarator: Declarator, val rvalue: Expression): AnyDeclarator() { //TODO rename
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return declarator.name()
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): TypeDesc = memoizeType {
        var pointerType = declspec.specifyType(typeHolder, declarator.pointers)

        pointerType = declarator.directDeclarator.resolveType(pointerType, declspec.storageClass(), typeHolder)
        assertion (!declspec.isTypedef) { "typedef is not supported here" }

        if (pointerType !is UncompletedArrayType) {
            typeHolder.addVar(name(), pointerType)
            return@memoizeType pointerType
        }

        when (rvalue) {
            is InitializerList -> {
                // Special case for array initialization without exact size like:
                // int a[] = {1, 2};
                // 'a' is array of 2 elements, not pointer to int

                val rvalueType = CArrayType(CArrayBaseType(pointerType.element(), rvalue.length().toLong()), listOf())
                typeHolder.addVar(name(), rvalueType)
                return@memoizeType rvalueType
            }
            is StringNode -> {
                // Special case for string initialization like:
                // char a[] = "hello";
                val rvalueType = rvalue.resolveType(typeHolder)
                typeHolder.addVar(name(), rvalueType)
                return@memoizeType rvalueType
            }
            else -> throw TypeResolutionException("Array size is not specified")
        }
    }
}

data object EmptyDeclarator : AnyDeclarator() {
    override fun name(): String = ""

    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): TypeDesc {
        throw TypeResolutionException("Empty declarator is not supported")
    }
}

data class StructDeclarator(val declarator: AnyDeclarator, val expr: Expression): AnyDeclarator() {
    override fun <T> accept(visitor: DeclaratorVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): TypeDesc = memoizeType {
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

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): CFunctionType = memoizeType {
        assertion(specifier === declspec) { "specifier should be the same" }

        var pointerType = declspec.specifyType(typeHolder, declarator.pointers)

        pointerType = declarator.directDeclarator.resolveType(pointerType, declspec.storageClass(), typeHolder)
        assertion(!declspec.isTypedef) { "typedef is not supported here" }

        assertion(pointerType is CFunctionType) { "function type expected" }
        typeHolder.addFunctionType(name(), pointerType as CFunctionType)
        return@memoizeType pointerType
    }

    fun resolveType(typeHolder: TypeHolder): CFunctionType {
        return declareType(specifier, typeHolder)
    }

    override fun <T> accept(visitor: DeclaratorVisitor<T>): T = visitor.visit(this)
}