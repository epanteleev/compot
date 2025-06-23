package parser.nodes

import tokenizer.Position
import tokenizer.tokens.Identifier


// Special case for first parameter of DirectDeclarator
sealed class DirectDeclaratorEntry {
    abstract fun begin(): Position
    abstract fun name(): String
}

data class FunctionDeclarator(val declarator: Declarator): DirectDeclaratorEntry() {
    override fun begin(): Position = declarator.begin()

    override fun name(): String = declarator.name()
}

data class DirectVarDeclarator(private val ident: Identifier): DirectDeclaratorEntry() {
    override fun begin(): Position = ident.position()
    override fun name(): String = ident.str()
}