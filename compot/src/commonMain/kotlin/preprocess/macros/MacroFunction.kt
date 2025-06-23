package preprocess.macros

import tokenizer.*
import tokenizer.tokens.CToken

class MacroFunction(name: String, internal val argNames: CTokenList, internal val value: TokenList): Macros(name) {
    override fun first(): CToken {
        return value.first() as CToken
    }

    override fun tokenString(): String {
        val builder = StringBuilder("#define ")
        builder.append(name)
            .append('(')

        argNames.joinTo(builder, ",") { it.str() }
        builder.append(')')

        value.joinTo(builder, " ") { it.str() }
        return builder.toString()
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as MacroFunction

        if (name != other.name) return false
        if (value != other.value) return false

        return true
    }
}