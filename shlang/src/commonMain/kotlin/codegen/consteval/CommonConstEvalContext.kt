package codegen.consteval

import parser.InvalidToken
import parser.ParserException
import tokenizer.tokens.CToken
import typedesc.TypeHolder


class CommonConstEvalContext<T: Number>(val typeHolder: TypeHolder, val enumerationValues: Map<String, Int> = hashMapOf()): ConstEvalContext<T> {
    override fun getVariable(name: CToken): T? {
        val value = enumerationValues[name.str()] ?: typeHolder.findEnumByEnumerator(name.str())
        if (value != null) {
            @Suppress("UNCHECKED_CAST")
            return value as T //TODO
        }
        return null
    }

    override fun callFunction(name: CToken, args: List<T>): T? {
        return null
    }

    override fun typeHolder(): TypeHolder {
        return typeHolder
    }
}

class ArraySizeConstEvalContext(val typeHolder: TypeHolder): ConstEvalContext<Long> {
    override fun getVariable(name: CToken): Long? {
        val value = typeHolder.findEnumByEnumerator(name.str())
        if (value != null) {
            return value.toLong()
        }
        return null
    }

    override fun callFunction(name: CToken, args: List<Long>): Long {
        val error = InvalidToken("Cannot consteval expression: found function", name)
        throw ParserException(error)
    }

    override fun typeHolder(): TypeHolder {
        return typeHolder
    }
}