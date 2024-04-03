package semantic

import types.CType
import tokenizer.CToken
import parser.nodes.Node


data class SymbolInfo(val scope: SymbolScope, val name: String, val type: CType, val node: Node, val token: CToken, val isIntrinsic: Boolean = false) {
    val desc get() = type.toString()
    val score: Int get() = scope.level
}