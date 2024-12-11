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

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as MacroFunction

        if (argNames != other.argNames) return false
        if (value != other.value) return false

        return true
    }
}