package semantic

import types.Type
import tokenizer.Token
import parser.nodes.Node


data class SymbolInfo(val scope: SymbolScope, val name: String, val type: Type, val node: Node, val token: Token, val isIntrinsic: Boolean = false) {
    val desc get() = type.toString()
    val score: Int get() = scope.level
}