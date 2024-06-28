package preprocess

import tokenizer.*
import common.forEachWith
import kotlin.jvm.JvmStatic

data class MacroExpansionException(override val message: String): Exception(message)


abstract class Macros(val name: String) {
    abstract fun first(): CToken

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Macros

        return name == other.name
    }

    companion object {
        @JvmStatic
        protected fun newTokenFrom(macrosNamePos: Position, tok: AnyToken): AnyToken {
            if (tok !is CToken) {
                return tok.cloneWith(macrosNamePos)
            }

            val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, tok.position() as OriginalPosition)
            return tok.cloneWith(preprocessedPosition)
        }
    }
}

class MacroDefinition(name: String): Macros(name) {
    override fun first(): CToken {
        throw MacroExpansionException("Macro definition cannot be expanded")
    }
}

class PredefinedMacros(name: String, private val callback: (Position) -> CToken): Macros(name) {
    override fun first(): CToken {
        val pos = Position.UNKNOWN
        return callback(pos)
    }

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

class MacroReplacement(name: String, private val value: TokenList): Macros(name) {
    override fun first(): CToken {
        return value.first() as CToken
    }

    fun cloneContentWith(macrosNamePos: Position): TokenList {
        val result = TokenList()
        for (tok in value) {
            result.add(newTokenFrom(macrosNamePos, tok))
        }

        return result
    }
}

class MacroFunction(name: String, private val argNames: CTokenList, private val value: TokenList): Macros(name) {
    override fun first(): CToken {
        return value.first() as CToken
    }

    private fun seekNonSpace(idx: AnyToken?): AnyToken {
        var current: AnyToken? = idx
        do {
            if (current == null) {
                throw MacroExpansionException("Invalid macro expansion")
            }

            if (current is CToken) {
                return current
            }
            current = current.next()
        } while (true)
    }

    private fun concatTokens(result: TokenList, argToValue: Map<CToken, TokenList>, current: CToken): AnyToken? {
        val i = seekNonSpace(current.next())
        val value = argToValue[i] ?: throw MacroExpansionException("Invalid macro expansion: ## without argument")

        val arg1 = result.findLast { it is CToken } as CToken

        val str = value.joinToString("") { it.str() }
        val str1 = arg1.str()
        result.remove(arg1)
        if (result.last() is Indent) {
            result.removeLast()
        }
        result.add(Identifier(str1 + str, current.position())) //TODO identifier not everytime
        return i.next()
    }

    private fun stringify(result: TokenList, argToValue: Map<CToken, TokenList>, current: CToken): AnyToken? {
        val i = seekNonSpace(current.next())
        val value = argToValue[i] ?: throw MacroExpansionException("Invalid macro expansion: # without argument")
        val str = value.joinToString("") { it.str() }
        result.add(StringLiteral("\"" + str + "\"", current.position()))
        return i.next()
    }

    private fun evaluateSubstitution(args: List<TokenList>): Map<CToken, TokenList> {
        val res = mutableMapOf<CToken, TokenList>()
        this.argNames.forEachWith(args) { arg, value ->
            res[arg] = value
        }
        return res
    }

    fun cloneContentWith(macrosNamePos: Position, args: List<TokenList>): TokenList {
        val argToValue = evaluateSubstitution(args)

        val result = TokenList()
        var current = value.firstOrNull()
        do {
            if (current == null) {
                break
            }
            if (current !is CToken) {
                result.add(current.cloneWith(PreprocessedPosition.UNKNOWN))
                current = current.next()
                continue
            }

            if (current.str() == "##") {
                current = concatTokens(result, argToValue, current)
                continue
            } else if (current.str() == "#") {
                current = stringify(result, argToValue, current)
                continue
            }

            val value = argToValue[current]
            if (value == null) {
                result.add(newTokenFrom(macrosNamePos, current))
                current = current.next()
                continue
            }

            val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, current.position() as OriginalPosition)
            for (tok in value) {
                result.add(tok.cloneWith(preprocessedPosition))
            }
            current = current.next()
        } while (current != null)
        return result
    }
}