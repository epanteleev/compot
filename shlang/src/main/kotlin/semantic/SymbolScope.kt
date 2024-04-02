package semantic

import parser.nodes.Node
import tokenizer.CToken
import types.FunctionType
import types.CType


class SymbolScope(val parent: SymbolScope?, var start: Int = -1, var end: Int = -1) {
    val level: Int = if (parent != null) parent.level + 1 else 0
    val isGlobal get() = parent == null

    val children = arrayListOf<SymbolScope>()
    val symbols = LinkedHashMap<String, SymbolInfo>()

    fun createInfo(name: String, type: CType, node: Node, token: CToken) = SymbolInfo(this, name, type, node, token)

    init {
        parent?.children?.add(this)
    }

    fun registerInfo(name: String, type: CType, node: Node, token: CToken) = register(createInfo(name, type, node, token))

    private fun register(symbol: SymbolInfo) {
        symbols[symbol.name] = symbol
    }

    operator fun get(symbol: String): SymbolInfo? {
        return getHere(symbol) ?: parent?.get(symbol)
    }

    fun getHere(symbol: String): SymbolInfo? {
        return this.symbols[symbol]
    }

    fun getAllSymbolNames(out: MutableSet<String> = mutableSetOf()): Set<String> {
        out += symbols.keys
        parent?.getAllSymbolNames(out)
        return out
    }

    override fun toString(): String =
        "SymbolScope(level=$level, symbols=${symbols.keys}, children=${children.size}, parent=${parent != null}, start=$start, end=$end)"
}


class FunctionScope {
    var name: String = ""
    var type: FunctionType? = null
    var hasGoto: Boolean = false
    val rettype: CType get() = type?.retType ?: CType.UNRESOLVED
}