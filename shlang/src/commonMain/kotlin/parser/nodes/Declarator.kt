package parser.nodes

import types.*
import common.assertion
import parser.nodes.visitors.*
import tokenizer.Position
import typedesc.TypeDesc
import typedesc.TypeHolder
import typedesc.TypeResolutionException
import typedesc.VarDescriptor


sealed class AnyDeclarator: Node() {
    private var cachedType: VarDescriptor? = null

    abstract fun name(): String
    abstract fun<T> accept(visitor: DeclaratorVisitor<T>): T
    internal abstract fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor

    fun varDescriptor(): VarDescriptor {
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
    override fun begin(): Position = directDeclarator.begin()
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return directDeclarator.name()
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        val declspecType = declspec.specifyType(typeHolder, pointers)
        val type = directDeclarator.resolveType(declspecType.typeDesc, typeHolder)
        if (declspec.isTypedef) {
            assertion(declspecType.storageClass == null) { "typedef with storage class is not supported" }

            typeHolder.addTypedef(name(), type)
            return@memoizeType VarDescriptor(type, declspecType.storageClass)
        }

        val varDesc = VarDescriptor(type, declspecType.storageClass)
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

data class InitDeclarator(val declarator: Declarator, val rvalue: Expression): AnyDeclarator() {
    override fun begin(): Position = declarator.begin()
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return declarator.name()
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        val declspecType = declspec.specifyType(typeHolder, declarator.pointers)

        val type = declarator.directDeclarator.resolveType(declspecType.typeDesc, typeHolder)
        assertion (!declspec.isTypedef) { "typedef is not supported here" }

        val baseType = type.cType()
        if (baseType !is CUncompletedArrayType) {
            return@memoizeType typeHolder.addVar(name(), VarDescriptor(type, declspecType.storageClass))
        }

        when (rvalue) {
            is InitializerList -> {
                // Special case for array initialization without exact size like:
                // int a[] = {1, 2};
                // 'a' is array of 2 elements, not pointer to int

                when (val initializerType = rvalue.resolveType(typeHolder)) {
                    is InitializerType -> {
                        val rvalueType = TypeDesc.from(CArrayType(baseType.element(), rvalue.length().toLong()), listOf())
                        return@memoizeType typeHolder.addVar(name(), VarDescriptor(rvalueType, declspecType.storageClass))
                    }
                    is CStringLiteral -> {
                        val rvalueType = TypeDesc.from(CArrayType(baseType.element(), initializerType.dimension + 1), listOf())
                        return@memoizeType typeHolder.addVar(name(), VarDescriptor(rvalueType, declspecType.storageClass))
                    }
                    else -> throw TypeResolutionException("Array size is not specified: type=$initializerType", declarator.begin())
                }
            }
            is StringNode -> {
                // Special case for string initialization like:
                // char a[] = "hello";
                return@memoizeType typeHolder.addVar(name(), VarDescriptor(TypeDesc.from(rvalue.resolveType(typeHolder)), declspecType.storageClass))
            }
            else -> throw TypeResolutionException("Array size is not specified", declarator.begin())
        }
    }
}

class EmptyDeclarator(private val where: Position) : AnyDeclarator() {
    override fun begin(): Position = where
    override fun name(): String = ""

    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor {
        throw TypeResolutionException("Empty declarator is not supported", begin())
    }
}

data class StructDeclarator(val declarator: AnyDeclarator, val expr: Expression): AnyDeclarator() {
    override fun begin(): Position = declarator.begin()
    override fun <T> accept(visitor: DeclaratorVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        if(expr !is EmptyExpression) {
            println("Warning: bit field is not supported")
        }

        return@memoizeType declarator.declareType(declspec, typeHolder)
    }

    override fun name(): String = declarator.name()
}

data class FunctionNode(val specifier: DeclarationSpecifier,
                        val declarator: Declarator,
                        val body: Statement) : AnyDeclarator() {
    override fun begin(): Position = specifier.begin()

    override fun name(): String {
        return declarator.directDeclarator.decl.name()
    }

    fun functionDeclarator(): ParameterTypeList {
        return declarator.directDeclarator.directDeclaratorParams[0] as ParameterTypeList
    }

    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        assertion(specifier === declspec) { "specifier should be the same" }

        val declspecType = declspec.specifyType(typeHolder, declarator.pointers)

        val type = declarator.directDeclarator.resolveType(declspecType.typeDesc, typeHolder)
        assertion(!declspec.isTypedef) { "typedef is not supported here" }

        val baseType = type.cType()
        assertion(baseType is CFunctionType) { "function type expected" }
        return@memoizeType typeHolder.addFunctionType(name(), VarDescriptor(type, declspecType.storageClass))
    }

    fun resolveType(typeHolder: TypeHolder): CFunctionType {
        return declareType(specifier, typeHolder).typeDesc.cType() as CFunctionType
    }

    override fun <T> accept(visitor: DeclaratorVisitor<T>): T = visitor.visit(this)
}

data class DirectDeclarator(val decl: DirectDeclaratorFirstParam, val directDeclaratorParams: List<DirectDeclaratorParam>): AnyDeclarator() {
    override fun begin(): Position = decl.begin()
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)
    override fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder, ): VarDescriptor {
        TODO("Not yet implemented")
    }

    private fun resolveAllDecl(baseType: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        var currentType = baseType
        for (decl in directDeclaratorParams.reversed()) {
            when (decl) {
                is ArrayDeclarator -> {
                    currentType = decl.resolveType(currentType, typeHolder)
                }

                is ParameterTypeList -> {
                    val abstractType = decl.resolveType(currentType, typeHolder)
                    currentType = TypeDesc.from(CFunctionType(name(), abstractType.cType() as AbstractCFunction), abstractType.qualifiers())
                }

                else -> throw IllegalStateException("Unknown declarator $decl")
            }
        }
        return currentType
    }

    fun resolveType(baseType: TypeDesc, typeHolder: TypeHolder): TypeDesc = when (decl) {
        is FunctionPointerDeclarator -> {
            assertion(directDeclaratorParams.size == 1) { "Function pointer should have only one parameter" }
            val fnDecl = directDeclaratorParams[0] as ParameterTypeList
            val type = fnDecl.resolveType(baseType, typeHolder)
            decl.resolveType(type, typeHolder)
        }
        is DirectVarDeclarator -> resolveAllDecl(baseType, typeHolder)
    }

    override fun name(): String = decl.name()
}