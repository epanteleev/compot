package parser

import tokenizer.CToken


data class ProgramMessage(val message: String, val token: CToken) {

    override fun toString(): String {
        return "Error: $message at ${token.line()}:${token.pos()}, but found '${token.str()}'"
    }
}