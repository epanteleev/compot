package preprocess.macros

import tokenizer.*
import tokenizer.tokens.CToken

class MacroFunction(name: String, internal val argNames: CTokenList, internal val value: TokenList): Macros(name) {
    override fun first(): CToken {
        return value.first() as CToken
    }

    override fun tokenString(): String {
        return "#define $name(${argNames.joinToString(", ") { it.str() }}) ${value.joinToString(" ") { it.str() }}"
    }
}