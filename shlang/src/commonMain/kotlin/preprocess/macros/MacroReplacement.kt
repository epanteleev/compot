package preprocess.macros

import tokenizer.Position
import tokenizer.TokenList
import tokenizer.tokens.CToken


class MacroReplacement(name: String, val value: TokenList): Macros(name) {
    override fun first(): CToken {
        return value.first() as CToken
    }

    override fun tokenString(): String {
        val builder = StringBuilder("#define ")
        builder.append(name)
            .append(' ')
        value.joinTo(builder) { it.str() }

        return builder.toString()
    }

    fun substitute(macrosNamePos: Position): TokenList {
        val result = TokenList()
        for (tok in value) {
            result.add(newTokenFrom(name, macrosNamePos, tok))
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
        return name.hashCode()
    }
}
