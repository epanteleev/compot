package parser.nodes

import parser.nodes.visitors.*
import tokenizer.Position
import tokenizer.tokens.Punctuator


sealed class AnyParameter {
    abstract fun begin(): Position
    abstract fun<T> accept(visitor: ParameterVisitor<T>): T
}

sealed class AnyParamDeclarator
data class EmptyParamDeclarator(val where: Position) : AnyParamDeclarator()
class ParamAbstractDeclarator(val abstractDeclarator: AbstractDeclarator) : AnyParamDeclarator()
class ParamDeclarator(val declarator: Declarator) : AnyParamDeclarator()

data class Parameter(val declspec: DeclarationSpecifier, val paramDeclarator: AnyParamDeclarator) : AnyParameter() {
    override fun begin(): Position = declspec.begin()
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)

    fun name(): String? {
        if (paramDeclarator !is ParamDeclarator) {
            return null
        }

        return paramDeclarator.declarator.directDeclarator.decl.name()
    }
}

class ParameterVarArg(private val punctuation: Punctuator): AnyParameter() {
    override fun begin(): Position = punctuation.position()
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)
}