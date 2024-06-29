package parser.nodes

import gen.consteval.ConstEvalContext
import gen.consteval.ConstEvalExpression
import parser.ParserException
import parser.InvalidToken
import parser.nodes.visitors.DirectDeclaratorParamVisitor
import tokenizer.CToken
import tokenizer.Identifier
import types.*

abstract class DirectDeclaratorParam: Node() {
    abstract fun resolveType(baseType: CType, typeHolder: TypeHolder): CType
    abstract fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>): T
}

data class ArrayDeclarator(val constexpr: Expression) : DirectDeclaratorParam() {
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        if (constexpr is EmptyExpression) {
            return CPointerType(baseType)
        }

        val size = ConstEvalExpression.eval(constexpr, ArraySizeEvalContext(typeHolder))
        return CompoundType(CArrayType(baseType, size))
    }

    companion object {
        class ArraySizeEvalContext(val typeHolder: TypeHolder): ConstEvalContext {
            override fun getVariable(name: CToken): Int {
                val error = InvalidToken("Cannot consteval expression: found variable", name)
                throw ParserException(error)
            }

            override fun callFunction(name: CToken, args: List<Int>): Int {
                val error = InvalidToken("Cannot consteval expression: found function", name)
                throw ParserException(error)
            }

            override fun typeHolder(): TypeHolder {
                return typeHolder
            }
        }
    }
}

data class IndentifierList(val list: List<IdentNode>): DirectDeclaratorParam() {
    override fun <T> accept(visitor: DirectDeclaratorParamVisitor<T>): T = visitor.visit(this)

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }
}

data class ParameterTypeList(val params: List<AnyParameter>): DirectDeclaratorParam() {
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)

    fun resolveType(typeHolder: TypeHolder): CType {
        params.forEach { it.resolveType(typeHolder) }
        return CType.UNKNOWN
    }

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): AbstractCFunctionType {
        val params = resolveParams(typeHolder)
        return AbstractCFunctionType(baseType, params, isVarArg())
    }

    fun params(): List<String> {
        return params.map {
            when (it) {
                is Parameter -> it.name()
                is ParameterVarArg -> "..."
                else -> throw IllegalStateException("Unknown parameter $it")
            }
        }
    }

    private fun isVarArg(): Boolean {
        return params.any { it is ParameterVarArg }
    }

    private fun resolveParams(typeHolder: TypeHolder): List<CType> {
        val paramTypes = mutableListOf<CType>()
        for (param in params) {
            when (param) {
                is Parameter -> {
                    val type = param.resolveType(typeHolder)
                    paramTypes.add(type)
                }
                is ParameterVarArg -> {}
                else -> throw IllegalStateException("Unknown parameter $param")
            }
        }

        return paramTypes
    }
}

// Special case for first parameter of DirectDeclarator
abstract class DirectDeclaratorFirstParam : DirectDeclaratorParam() {
    abstract fun name(): String
}

data class FunctionPointerDeclarator(val declarator: Declarator): DirectDeclaratorFirstParam() {
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)

    override fun name(): String = declarator.name()

    fun declarator(): Declarator = declarator

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }
}

data class DirectVarDeclarator(val ident: Identifier): DirectDeclaratorFirstParam() {
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }
}