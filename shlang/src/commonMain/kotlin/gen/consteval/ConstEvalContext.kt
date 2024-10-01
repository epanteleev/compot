package gen.consteval

import tokenizer.tokens.CToken
import types.TypeHolder

interface ConstEvalContext<T: Number> {
    fun getVariable(name: CToken): T?
    fun callFunction(name: CToken, args: List<T>): T?
    fun typeHolder(): TypeHolder
}