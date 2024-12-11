package preprocess.macros

import tokenizer.tokens.CToken

class MacroDefinition(name: String): Macros(name) {
    override fun first(): CToken {
        throw MacroExpansionException("Macro definition cannot be expanded")
    }

    override fun tokenString(): String {
        return "#define $name"
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false
        return true
    }
}