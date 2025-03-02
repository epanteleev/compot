package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder
import types.CType

sealed class Initializer {
    abstract fun begin(): Position
    abstract fun resolveType(typeHolder: TypeHolder): CType
}

class ExpressionInitializer(val expr: Expression): Initializer() {
    override fun begin(): Position = expr.begin()
    override fun resolveType(typeHolder: TypeHolder): CType {
        return expr.resolveType(typeHolder)
    }
}

class InitializerListInitializer(val list: InitializerList): Initializer() {
    override fun begin(): Position = list.begin()
    override fun resolveType(typeHolder: TypeHolder): CType {
        return list.resolveType(typeHolder)
    }
}