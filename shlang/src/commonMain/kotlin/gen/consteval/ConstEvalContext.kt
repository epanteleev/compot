package gen.consteval

import tokenizer.CToken
import types.TypeHolder

interface ConstEvalContext {
    fun getVariable(name: CToken): Int
    fun callFunction(name: CToken, args: List<Int>): Int
    fun typeHolder(): TypeHolder
}