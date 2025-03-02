package parser.nodes

import tokenizer.Position
import tokenizer.tokens.Identifier

data class IdentNode(private val str: Identifier) {
    fun begin(): Position = str.position()
    fun str(): String = str.str()
}