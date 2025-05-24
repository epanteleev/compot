package parser.nodes

import tokenizer.Position


sealed class AnyStructDeclaratorItem {
    abstract fun begin(): Position
}

class StructDeclaratorItem(val expr: Declarator): AnyStructDeclaratorItem() {
    override fun begin(): Position = expr.begin()
}

class EmptyStructDeclaratorItem(private val name: String, private val where: Position): AnyStructDeclaratorItem() {
    override fun begin(): Position = where
    fun name(): String = name
}

class StructDeclarator(val declarator: AnyStructDeclaratorItem, val expr: Expression) {
    fun begin(): Position = declarator.begin()

    fun name(): String = when(declarator) {
        is StructDeclaratorItem -> declarator.expr.name()
        is EmptyStructDeclaratorItem -> ""
    }
}

class StructField(val declspec: DeclarationSpecifier, val declarators: List<StructDeclarator>) {
    fun begin(): Position = declspec.begin()
}