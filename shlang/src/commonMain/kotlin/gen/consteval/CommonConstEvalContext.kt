package gen.consteval

import parser.InvalidToken
import parser.ParserException
import tokenizer.CToken
import types.TypeHolder


class CommonConstEvalContext<T: Number>(val typeHolder: TypeHolder): ConstEvalContext<T> {
    override fun getVariable(name: CToken): T {
        val error = InvalidToken("Cannot consteval expression: found variable", name)
        throw ParserException(error)
    }

    override fun callFunction(name: CToken, args: List<T>): T {
        val error = InvalidToken("Cannot consteval expression: found function", name)
        throw ParserException(error)
    }

    override fun typeHolder(): TypeHolder {
        return typeHolder
    }
}