package gen.consteval

import parser.InvalidToken
import parser.ParserException
import tokenizer.CToken
import types.TypeHolder


class CommonConstEvalContext<T: Number>(val typeHolder: TypeHolder, val enumerationValues: Map<String, Int> = hashMapOf<String, Int>()): ConstEvalContext<T> {
    override fun getVariable(name: CToken): T? {
        val value = enumerationValues[name.str()]
        if (value != null) {
            return value as T //TODO
        }
        return null
    }

    override fun callFunction(name: CToken, args: List<T>): T {
        val error = InvalidToken("Cannot consteval expression: found function", name)
        throw ParserException(error)
    }

    override fun typeHolder(): TypeHolder {
        return typeHolder
    }
}