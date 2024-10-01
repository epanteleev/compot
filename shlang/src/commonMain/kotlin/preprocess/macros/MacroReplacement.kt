package preprocess.macros

import tokenizer.Position
import tokenizer.TokenList
import tokenizer.tokens.CToken


class MacroReplacement(name: String, val value: TokenList): Macros(name) {
    override fun first(): CToken {
        return value.first() as CToken
    }

    override fun tokenString(): String {
        return "#define $name ${value.joinToString("") { it.str() }}"
    }

    fun substitute(macrosNamePos: Position): TokenList {
        val result = TokenList()
        for (tok in value) {
            result.add(newTokenFrom(macrosNamePos, tok))
        }

        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MacroReplacement

        if (name != other.name) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}
