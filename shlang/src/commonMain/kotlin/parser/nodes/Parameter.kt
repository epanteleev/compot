package parser.nodes

import types.CType
import types.TypeHolder
import parser.nodes.visitors.*


abstract class AnyParameter : Node(), Resolvable {
    abstract fun<T> accept(visitor: ParameterVisitor<T>): T
}

data class Parameter(val declspec: DeclarationSpecifier, val declarator: AnyDeclarator) : AnyParameter() {
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)

    fun name(): String {
        val varNode = declarator as Declarator
        return varNode.directDeclarator.decl.name()
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        val type = declspec.resolveType(typeHolder)
        return when (declarator) {
            is Declarator -> declarator.resolveType(type, typeHolder)
            is EmptyDeclarator -> type
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