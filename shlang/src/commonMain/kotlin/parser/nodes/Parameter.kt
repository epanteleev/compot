package parser.nodes

import typedesc.TypeDesc
import typedesc.TypeHolder
import parser.nodes.visitors.*
import tokenizer.Position
import tokenizer.tokens.Punctuator


sealed class AnyParameter {
    abstract fun begin(): Position
    abstract fun resolveType(typeHolder: TypeHolder): TypeDesc
    abstract fun<T> accept(visitor: ParameterVisitor<T>): T
}

sealed class AnyParamDeclarator
class EmptyParamDeclarator(val where: Position) : AnyParamDeclarator()
class ParamAbstractDeclarator(val abstractDeclarator: AbstractDeclarator) : AnyParamDeclarator()
class ParamDeclarator(val declarator: Declarator) : AnyParamDeclarator()

data class Parameter(val declspec: DeclarationSpecifier, val declarator: AnyParamDeclarator) : AnyParameter() {
    override fun begin(): Position = declspec.begin()
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)

    fun name(): String {
        if (declarator !is ParamDeclarator) {
            throw IllegalStateException("Expected declarator, but got $declarator")
        }

        return declarator.declarator.directDeclarator.decl.name()
    }

    override fun resolveType(typeHolder: TypeHolder): TypeDesc {
        val type = declspec.specifyType(typeHolder, listOf()).typeDesc
        return when (declarator) {
            is ParamDeclarator -> declarator.declarator.declareType(declspec, typeHolder).typeDesc
            is ParamAbstractDeclarator -> declarator.abstractDeclarator.resolveType(type, typeHolder)
            is EmptyParamDeclarator -> type
        }
    }
}

class ParameterVarArg(private val punctuator: Punctuator): AnyParameter() {
    override fun begin(): Position = punctuator.position()
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)
    override fun resolveType(typeHolder: TypeHolder): TypeDesc {
        throw IllegalStateException("VarArg type is not resolved")
    }
}