package parser

import tokenizer.AnyToken
import tokenizer.CToken


data class ProgramMessage(val message: String, val token: AnyToken) {
    override fun toString(): String {
        return when (token) {
            is CToken -> "Error: $message at ${token.line()}:${token.pos()}, but found '${token.str()}' in '${token.position().filename()}'"
            else -> "Error: $message at $token"
        }
    }
}