package preprocess.macros

import tokenizer.tokens.CToken

class MacroDefinition(name: String): Macros(name) {
    override fun first(): CToken {
        throw MacroExpansionException("Macro definition cannot be expanded")
    }

    override fun tokenString(): String {
        return "#define $name"
    }
}