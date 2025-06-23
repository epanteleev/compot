package parser.nodes

import tokenizer.Position


data class DirectDeclarator(val decl: DirectDeclaratorEntry, val directDeclaratorParams: List<DirectDeclaratorParam>) {
    fun begin(): Position = decl.begin()
    fun name(): String = decl.name()
}