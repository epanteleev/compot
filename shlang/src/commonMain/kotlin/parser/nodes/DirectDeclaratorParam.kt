package parser.nodes

import types.*
import typedesc.*
import codegen.consteval.*
import tokenizer.tokens.Identifier
import parser.nodes.visitors.DirectDeclaratorParamVisitor
import tokenizer.Position


sealed class DirectDeclaratorParam: Node() {
    abstract fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc
    abstract fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>): T
}

data class AbstractDeclarator(val pointers: List<NodePointer>, val directAbstractDeclarators: List<DirectDeclaratorParam>?) : DirectDeclaratorParam() {   //TODO
    override fun begin(): Position {
        if (pointers.isEmpty()) {
            return directAbstractDeclarators?.first()?.begin() ?: throw IllegalStateException("No pointers and no declarators")
        }

        return pointers.first().begin()
    }

    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>): T = visitor.visit(this)

    override fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        var pointerType = typeDesc.cType()
        for (pointer in pointers) {
            pointerType = CPointer(pointerType, pointer.property())
        }
        var newTypeDesc = TypeDesc.from(pointerType)
        if (directAbstractDeclarators == null) {
            return newTypeDesc
        }

        for (abstractDeclarator in directAbstractDeclarators.reversed()) {
            newTypeDesc = when (abstractDeclarator) {
                is ArrayDeclarator, is AbstractDeclarator, is ParameterTypeList -> {
                    abstractDeclarator.resolveType(newTypeDesc, typeHolder)
                }
                else -> throw IllegalStateException("Unknown declarator $abstractDeclarator")
            }
        }

        return newTypeDesc
    }
}

data class ArrayDeclarator(val constexpr: Expression) : DirectDeclaratorParam() {
    override fun begin(): Position = constexpr.begin()
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        if (constexpr is EmptyExpression) {
            return TypeDesc.from(CUncompletedArrayType(typeDesc))
        }

        val ctx = ArraySizeConstEvalContext(typeHolder)
        val size = ConstEvalExpression.eval(constexpr, TryConstEvalExpressionLong(ctx))
            ?: return TypeDesc.from(CUncompletedArrayType(typeDesc))
        return TypeDesc.from(CArrayType(typeDesc, size))
    }
}

data class IdentifierList(val list: List<IdentNode>): DirectDeclaratorParam() {
    override fun begin(): Position = list.first().begin()
    override fun <T> accept(visitor: DirectDeclaratorParamVisitor<T>): T = visitor.visit(this)

    override fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        TODO("Not yet implemented")
    }
}

data class ParameterTypeList(val params: List<AnyParameter>): DirectDeclaratorParam() {
    override fun begin(): Position = params.first().begin()
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        val params = resolveParams(typeHolder)
        return TypeDesc.from(AbstractCFunction(typeDesc, params, isVarArg()), arrayListOf())
    }

    fun params(): List<String> {
        if (params.isEmpty()) {
            return emptyList()
        }
        if (params.size == 1 && params[0] is Parameter) {
            val param = params[0] as Parameter
            if (param.declarator is EmptyDeclarator) {
                return emptyList()
            }
        }
        return params.map {
            when (it) {
                is Parameter -> it.name()
                is ParameterVarArg -> "..."
            }
        }
    }

    private fun isVarArg(): Boolean {
        return params.any { it is ParameterVarArg }
    }

    private fun resolveParams(typeHolder: TypeHolder): List<TypeDesc> {
        val paramTypes = mutableListOf<TypeDesc>()
        if (params.size == 1) {
            val type = params[0].resolveType(typeHolder)
            // Special case for void
            // Pattern: 'void f(void)' can be found in the C program.
            return if (type.cType() == VOID) {
                emptyList()
            } else {
                listOf(type)
            }
        }
        for (param in params) {
            when (param) {
                is Parameter -> {
                    val type = param.resolveType(typeHolder)
                    paramTypes.add(type)
                }
                is ParameterVarArg -> {}
            }
        }

        return paramTypes
    }
}

// Special case for first parameter of DirectDeclarator
sealed class DirectDeclaratorFirstParam : DirectDeclaratorParam() {
    abstract fun name(): String
}

data class FunctionPointerDeclarator(val declarator: Declarator): DirectDeclaratorFirstParam() {
    override fun begin(): Position = declarator.begin()
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)

    override fun name(): String = declarator.name()

    override fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        val cType = if (typeDesc.cType() is AbstractCFunction) {
            TypeDesc.from(CPointer(typeDesc.cType() as AbstractCFunction, setOf()), listOf())
        } else {
            typeDesc
        }
        return declarator.directDeclarator.resolveType(cType, typeHolder)
    }
}

data class DirectVarDeclarator(val ident: Identifier): DirectDeclaratorFirstParam() {
    override fun begin(): Position = ident.position()
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()

    override fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        TODO("Not yet implemented")
    }
}