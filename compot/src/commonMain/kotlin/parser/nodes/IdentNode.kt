package parser.nodes

import tokenizer.Position
import tokenizer.tokens.Identifier

class IdentNode internal constructor(private val str: Identifier) {
    fun begin(): Position = str.position()
    fun str(): String = str.str()
}