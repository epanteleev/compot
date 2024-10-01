package tokenizer

import tokenizer.tokens.*
import common.LeakedLinkedList


class TokenList: LeakedLinkedList<AnyToken>() {
    fun kill(token: AnyToken): AnyToken? {
        val next = token.next()
        remove(token)
        return next
    }
}

fun tokenListOf(vararg tokens: AnyToken): TokenList {
    val list = TokenList()
    tokens.forEach { list.add(it) }
    return list
}

class CTokenList: LeakedLinkedList<CToken>()