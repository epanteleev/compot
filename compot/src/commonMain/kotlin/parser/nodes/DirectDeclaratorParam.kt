package parser.nodes

import parser.nodes.visitors.DirectDeclaratorParamVisitor
import tokenizer.Position


sealed interface DirectDeclaratorParam {
    fun begin(): Position
    fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>): T
}

sealed interface AbstractDirectDeclaratorParam {
    fun begin(): Position
    fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>): T
}

class AbstractDeclarator(private val begin: Position, val pointers: List<NodePointer>, val directAbstractDeclarators: List<AbstractDirectDeclaratorParam>?): AbstractDirectDeclaratorParam {   //TODO
    override fun begin(): Position = begin
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>): T = visitor.visit(this)
}

data class ArrayDeclarator(val constexpr: Expression) : DirectDeclaratorParam, AbstractDirectDeclaratorParam {
    override fun begin(): Position = constexpr.begin()
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)
}

data class IdentifierList(val list: List<IdentNode>): DirectDeclaratorParam {
    override fun begin(): Position = list.first().begin()
    override fun <T> accept(visitor: DirectDeclaratorParamVisitor<T>): T = visitor.visit(this)
}

class ParameterTypeList(private val begin: Position, val params: List<AnyParameter>): DirectDeclaratorParam, AbstractDirectDeclaratorParam {
    override fun begin(): Position = begin
    override fun<T> accept(visitor: DirectDeclaratorParamVisitor<T>) = visitor.visit(this)

    fun isVarArg(): Boolean {
        return params.lastOrNull() is ParameterVarArg
    }
}