package codegen.consteval

import tokenizer.tokens.CToken
import typedesc.TypeHolder

interface ConstEvalContext<T: Number> {
    fun getVariable(name: CToken): T?
    fun callFunction(name: CToken, args: List<T>): T?
    fun typeHolder(): TypeHolder
}