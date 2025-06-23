package tokenizer

import tokenizer.tokens.*
import common.LeakedLinkedList


class TokenList: LeakedLinkedList<AnyToken>() {
    fun kill(token: AnyToken): AnyToken? {
        val next = token.next()
        remove(token)
        return next
    }

    fun clone(): TokenList {
        val list = TokenList()
        for (token in this) {
            list.add(token.copy())
        }
        return list
    }
}

fun tokenListOf(vararg tokens: AnyToken): TokenList {
    val list = TokenList()
    tokens.forEach { list.add(it) }
    return list
}

class CTokenList: LeakedLinkedList<CToken>()