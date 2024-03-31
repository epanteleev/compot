package parser

import tokenizer.AnyToken

data class ProgramMessage(val message: String, val token: AnyToken) {

    override fun toString(): String {
        return "Error: $message at ${token.line()}:${token.pos()}, but found '${token.str()}'"
    }
}