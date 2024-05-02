package preprocess

import common.forEachWith
import tokenizer.*

data class MacroExpansionException(override val message: String): Exception(message)


abstract class Macros(val name: String) {
    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Macros

        return name == other.name
    }
}

class MacroDefinition(name: String): Macros(name)

class PredefinedMacros(name: String, private val callback: (Position) -> CToken): Macros(name) {
    fun cloneContentWith(macrosNamePos: Position): CToken {
        val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos,
            callback(macrosNamePos).position() as OriginalPosition)

        return callback(preprocessedPosition)
    }

    fun constEval(): Int {
        val pos = Position.UNKNOWN
        val token = callback(pos)
        if (token !is Numeric) {
            throw PreprocessorException("Predefined macro '$name' is not a number")
        }

        return token.data.toInt()
    }
}

class MacroReplacement(name: String, private val value: List<AnyToken>): Macros(name) {
    fun first(): CToken {
        return value.first() as CToken
    }

    private fun last(): CToken {
        return value.last() as CToken
    }

    fun cloneContentWith(macrosNamePos: Position): List<AnyToken> {
        val firstPos = first().position().pos()
        fun calculate(tok: AnyToken): AnyToken {
            if (tok !is CToken) {
                return tok
            }

            val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, tok.position() as OriginalPosition)
            return tok.cloneWith(preprocessedPosition)
        }

        return value.map { calculate(it) }
    }
}

class MacroFunction(name: String, private val args: List<CToken>, private val value: List<AnyToken>): Macros(name) {
    fun first(): CToken {
        return value.first() as CToken
    }

    private fun calculate(macrosNamePos: Position, tok: AnyToken): AnyToken {
        if (tok !is CToken) {
            return tok
        }

        val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, tok.position() as OriginalPosition)
        return tok.cloneWith(preprocessedPosition)
    }

    private fun seekNonSpace(idx: Int): Int {
        var i = idx
        while (i < value.size) {
            if (value[i] is CToken) {
                return i
            }
            i += 1
        }
        return i
    }

    private fun concatTokens(result: MutableList<AnyToken>, argToValue: Map<CToken, List<AnyToken>>, idx: Int): Int {
        val v = value[idx] as CToken
        val i = seekNonSpace(idx + 1)
        if (i >= value.size) {
            throw MacroExpansionException("Invalid macro expansion")
        }
        val arg = argToValue[value[i]] ?: let {
            result.add(v)
            return idx + 1
        }

        val arg1 = result.findLast { it is CToken } as CToken

        val str = arg.joinToString("") { it.str() }
        val str1 = arg1.str()
        result.remove(arg1)
        if (result.last() is Indent) {
            result.removeLast()
        }
        result.add(Ident(str1 + str, v.position() as OriginalPosition)) //TODO identifier not everytime
        return i + 1
    }

    private fun stringify(result: MutableList<AnyToken>, argToValue: Map<CToken, List<AnyToken>>, idx: Int): Int {
        val v = value[idx] as CToken
        val i = seekNonSpace(idx + 1)
        if (i >= value.size) {
            throw MacroExpansionException("Invalid macro expansion")
        }
        val arg = argToValue[value[i]] ?: let {
            result.add(v)
            return idx + 1
        }
        val str = arg.joinToString("") { it.str() }
        result.add(StringLiteral("\"" + str + "\"", v.position() as OriginalPosition))
        return i + 1
    }

    fun cloneContentWith(macrosNamePos: Position, args: List<List<CToken>>): List<AnyToken> {
        val argToValue = run {
            val res = mutableMapOf<CToken, List<CToken>>()
            this.args.forEachWith(args) { arg, value ->
                res[arg] = value
            }
            res
        }

        val result = mutableListOf<AnyToken>()
        var idx = 0
        do {
            val v = value[idx]
            if (v !is CToken) {
                result.add(v)
                idx += 1
                continue
            }

            if (v.str() == "##") {
                idx = concatTokens(result, argToValue, idx)
                continue
            } else if (v.str() == "#") {
                idx = stringify(result, argToValue, idx)
                continue
            }

            if (!argToValue.containsKey(v)) {
                result.add(calculate(macrosNamePos, v))
                idx += 1
                continue
            }

            val arg = argToValue[v]!!

            val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, v.position() as OriginalPosition)
            val r = arg.map { it.cloneWith(preprocessedPosition) }

            result.addAll(r)
            idx += 1
        } while (idx < value.size)

        return result
    }
}