package parser.nodes

import types.CType
import types.TypeHolder
import parser.nodes.visitors.*


abstract class AnyParameter : Node() {
    abstract fun resolveType(typeHolder: TypeHolder): CType
    abstract fun<T> accept(visitor: ParameterVisitor<T>): T
}

data class Parameter(val declspec: DeclarationSpecifier, val declarator: Node) : AnyParameter() {
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)

    fun name(): String {
        if (declarator !is Declarator) {
            throw IllegalStateException("Expected declarator, but got $declarator")
        }

        return declarator.directDeclarator.decl.name()
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        val type = declspec.specifyType(typeHolder, listOf())
        return when (declarator) {
            is Declarator         -> declarator.declareType(declspec, typeHolder)
            is AbstractDeclarator -> declarator.resolveType(type, typeHolder)
            is EmptyDeclarator    -> type
            else -> throw IllegalStateException("Unknown declarator $declarator")
        }
    }
}

class ParameterVarArg: AnyParameter() {
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)
    override fun resolveType(typeHolder: TypeHolder): CType {
        throw IllegalStateException("VarArg type is not resolved")
    }
}