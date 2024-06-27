package tokenizer

import common.LeakedLinkedList

class TokenList: LeakedLinkedList<AnyToken>()

class CTokenList: LeakedLinkedList<CToken>()