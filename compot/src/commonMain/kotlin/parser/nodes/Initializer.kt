package parser.nodes

import tokenizer.Position

sealed class Initializer {
    abstract fun begin(): Position
}

class ExpressionInitializer(val expr: Expression): Initializer() {
    override fun begin(): Position = expr.begin()
}

class InitializerListInitializer(val list: InitializerList): Initializer() {
    override fun begin(): Position = list.begin()
}