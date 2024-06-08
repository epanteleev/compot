package gen.consteval

import tokenizer.CToken

interface ConstEvalContext {
    fun getVariable(name: CToken): Int
    fun callFunction(name: CToken, args: List<Int>): Int
}