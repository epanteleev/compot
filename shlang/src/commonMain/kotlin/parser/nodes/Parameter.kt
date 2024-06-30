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
        val varNode = declarator as Declarator
        return varNode.directDeclarator.decl.name()
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        val type = declspec.specifyType(typeHolder)
        return when (declarator) {
            is Declarator         -> declarator.resolveType(declspec, typeHolder)
            is AbstractDeclarator -> declarator.resolveType(type, typeHolder)
            is EmptyDeclarator    -> type
            else -> throw IllegalStateException("Unknown declarator $declarator")
        }
    }
}

class ParameterVarArg: AnyParameter() {
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)
    override fun resolveType(typeHolder: TypeHolder): CType {
        return CType.UNKNOWN //TODO
    }
}