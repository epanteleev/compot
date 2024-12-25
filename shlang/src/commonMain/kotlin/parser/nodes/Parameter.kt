package parser.nodes

import typedesc.TypeDesc
import typedesc.TypeHolder
import parser.nodes.visitors.*
import tokenizer.Position
import tokenizer.tokens.Punctuator


sealed class AnyParameter : Node() {
    abstract fun resolveType(typeHolder: TypeHolder): TypeDesc
    abstract fun<T> accept(visitor: ParameterVisitor<T>): T
}

data class Parameter(val declspec: DeclarationSpecifier, val declarator: Node) : AnyParameter() {
    override fun begin(): Position = declspec.begin()
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)

    fun name(): String {
        if (declarator !is Declarator) {
            throw IllegalStateException("Expected declarator, but got $declarator")
        }

        return declarator.directDeclarator.decl.name()
    }

    override fun resolveType(typeHolder: TypeHolder): TypeDesc {
        val type = declspec.specifyType(typeHolder, listOf()).typeDesc
        return when (declarator) {
            is Declarator         -> declarator.declareType(declspec, typeHolder).typeDesc
            is AbstractDeclarator -> declarator.resolveType(type, typeHolder)
            is EmptyDeclarator    -> type
            else -> throw IllegalStateException("Unknown declarator $declarator")
        }
    }
}

class ParameterVarArg(val punctuator: Punctuator): AnyParameter() {
    override fun begin(): Position = punctuator.position()
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)
    override fun resolveType(typeHolder: TypeHolder): TypeDesc {
        throw IllegalStateException("VarArg type is not resolved")
    }
}