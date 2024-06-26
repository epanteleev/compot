package common

import tokenizer.AnyToken


abstract class AnyParser(protected val tokens: MutableList<AnyToken>) {
    protected var current: Int = 0
}