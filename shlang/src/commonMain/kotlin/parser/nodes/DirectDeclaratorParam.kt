package parser.nodes

import types.*
import typedesc.*
import codegen.consteval.*
import parser.nodes.visitors.DirectDeclaratorParamVisitor
import sema.SemanticAnalysis
import tokenizer.Position

sealed interface DirectDeclaratorParam {
    fun begin(): Position
    fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc
    fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>): T
}

sealed interface AbstractDirectDeclaratorParam {
    fun begin(): Position
    fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc
    fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>): T
}

data class AbstractDeclarator(val pointers: List<NodePointer>, val directAbstractDeclarators: List<AbstractDirectDeclaratorParam>?): AbstractDirectDeclaratorParam {   //TODO
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
            newTypeDesc = abstractDeclarator.resolveType(newTypeDesc, typeHolder)
        }

        return newTypeDesc
    }
}

data class ArrayDeclarator(val constexpr: Expression) : DirectDeclaratorParam, AbstractDirectDeclaratorParam {
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

data class IdentifierList(val list: List<IdentNode>): DirectDeclaratorParam {
    override fun begin(): Position = list.first().begin()
    override fun <T> accept(visitor: DirectDeclaratorParamVisitor<T>): T = visitor.visit(this)

    override fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        throw UnsupportedOperationException("Identifier list is not supported")
    }
}

data class ParameterTypeList(val params: List<AnyParameter>): DirectDeclaratorParam, AbstractDirectDeclaratorParam {
    override fun begin(): Position = params.first().begin()
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        val params = resolveParams(typeHolder)
        return TypeDesc.from(AbstractCFunction(typeDesc, params, isVarArg()), arrayListOf())
    }

    private fun isVarArg(): Boolean {
        return params.any { it is ParameterVarArg }
    }

    private fun resolveParams(typeHolder: TypeHolder): List<TypeDesc> {
        if (params.size == 1) {
            val first = params[0]
            if (first !is Parameter) {
                return emptyList()
            }
            val type =  SemanticAnalysis(typeHolder).resolveParameterType(first)
            // Special case for void
            // Pattern: 'void f(void)' can be found in the C program.
            return if (type.cType() == VOID) {
                emptyList()
            } else {
                listOf(type)
            }
        }

        val paramTypes = mutableListOf<TypeDesc>()
        for (param in params) {
            when (param) {
                is Parameter -> {
                    val type = SemanticAnalysis(typeHolder).resolveParameterType(param)
                    paramTypes.add(type)
                }
                is ParameterVarArg -> {}
            }
        }

        return paramTypes
    }
}