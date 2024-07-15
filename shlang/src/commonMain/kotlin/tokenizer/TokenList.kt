package tokenizer

import common.LeakedLinkedList

class TokenList: LeakedLinkedList<AnyToken>() {
    fun kill(token: AnyToken): AnyToken? {
        val next = token.next()
        remove(token)
        return next
    }

    fun lastNotSpace(): AnyToken? {
        var last = lastOrNull()
        while (last is AnySpaceToken) {
            last = last.prev()
        }
        return last
    }
}

fun tokenListOf(vararg tokens: AnyToken): TokenList {
    val list = TokenList()
    tokens.forEach { list.add(it) }
    return list
}

fun cTokenListOf(vararg tokens: CToken): CTokenList {
    val list = CTokenList()
    tokens.forEach { list.add(it) }
    return list
}

class CTokenList: LeakedLinkedList<CToken>()