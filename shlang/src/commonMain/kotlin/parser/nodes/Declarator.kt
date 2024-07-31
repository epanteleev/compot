package parser.nodes

import types.*
import common.assertion
import parser.nodes.visitors.*


sealed class AnyDeclarator: Node() {
    protected var cachedType: CType = CType.UNRESOlVED

    abstract fun name(): String
    abstract fun<T> accept(visitor: DeclaratorVisitor<T>): T
    internal abstract fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): CType

    fun cType(): CType {
        if (cachedType == CType.UNRESOlVED) {
            throw IllegalStateException("type is not resolved")
        }

        return cachedType
    }

    protected inline fun<reified T: CType> memoizeType(type: () -> T): T {
        if (cachedType == CType.UNRESOlVED) {
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

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): CType = memoizeType {
        var pointerType = declspec.specifyType(typeHolder)
        for (pointer in pointers) {
            pointerType = CPointerType(pointerType, pointer.property())
        }

        pointerType = directDeclarator.resolveType(pointerType, typeHolder)
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

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): CType = memoizeType {
        var pointerType = declspec.specifyType(typeHolder)
        for (pointer in declarator.pointers) {
            pointerType = CPointerType(pointerType, pointer.property())
        }

        pointerType = declarator.directDeclarator.resolveType(pointerType, typeHolder)
        assertion (!declspec.isTypedef) { "typedef is not supported here" }

        if (pointerType is CommonCArrayType && pointerType.hasUncompleted()) {
            // Special case for array initialization without exact size like:
            // int a[] = {1, 2};
            // 'a' is array of 2 elements, not pointer to int

            val rvalueType = rvalue.resolveType(typeHolder)
            typeHolder.addVar(name(), rvalueType)
            return@memoizeType rvalueType
        }
        typeHolder.addVar(name(), pointerType)
        return@memoizeType pointerType
    }
}

data object EmptyDeclarator : AnyDeclarator() {
    override fun name(): String = ""

    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): CType {
        throw TypeResolutionException("Empty declarator is not supported")
    }
}

data class StructDeclarator(val declarator: AnyDeclarator, val expr: Expression): AnyDeclarator() {
    override fun <T> accept(visitor: DeclaratorVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): CType = memoizeType {
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

        var pointerType = declspec.specifyType(typeHolder)
        for (pointer in declarator.pointers) {
            pointerType = CPointerType(pointerType, pointer.property())
        }

        pointerType = declarator.directDeclarator.resolveType(pointerType, typeHolder)
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