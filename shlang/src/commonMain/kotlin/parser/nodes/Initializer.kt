package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder
import types.CType
import types.CompletedType

sealed class Initializer {
    abstract fun begin(): Position
    abstract fun resolveType(typeHolder: TypeHolder): CType
}

class ExpressionInitializer(val expr: Expression): Initializer() {
    override fun begin(): Position = expr.begin()
    override fun resolveType(typeHolder: TypeHolder): CompletedType {
        return expr.resolveType(typeHolder)
    }
}

class InitializerListInitializer(val list: InitializerList): Initializer() {
    override fun begin(): Position = list.begin()
    override fun resolveType(typeHolder: TypeHolder): CType {
        return list.resolveType(typeHolder)
    }
}