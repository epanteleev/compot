package preprocess

import common.assertion
import tokenizer.*
import common.forEachWith
import preprocess.Macros.Companion.newTokenFrom
import kotlin.jvm.JvmStatic


data class MacroExpansionException(override val message: String): Exception(message)

abstract class Macros(val name: String) {
    abstract fun first(): CToken

    abstract fun tokenString(): String

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
        fun newTokenFrom(macrosNamePos: Position, tok: AnyToken): AnyToken {
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

    override fun tokenString(): String {
        return "#define $name"
    }
}

class PredefinedMacros(name: String, private val callback: (Position) -> TokenList): Macros(name) {
    override fun first(): CToken {
        return callback(Position.UNKNOWN).first() as CToken
    }

    override fun tokenString(): String {
        return "#define $name ${callback(Position.UNKNOWN).joinToString("") { it.str() }}"
    }

    fun cloneContentWith(macrosNamePos: Position): TokenList {
        val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, OriginalPosition.UNKNOWN)

        return callback(preprocessedPosition)
    }

    fun constEval(): Long {
        val tokens = callback(Position.UNKNOWN)
        val token = tokens.first()
        assertion(tokens.size == 1) { "invariant"}

        if (token !is Numeric) {
            throw PreprocessorException("Predefined macro '$name' is not a number")
        }

        return token.toNumberOrNull() as Long
    }
}

class MacroReplacement(name: String, val value: TokenList): Macros(name) {
    override fun first(): CToken {
        return value.first() as CToken
    }

    override fun tokenString(): String {
        return "#define $name ${value.joinToString("") { it.str() }}"
    }

    fun substitute(macrosNamePos: Position): TokenList {
        val result = TokenList()
        for (tok in value) {
            result.add(newTokenFrom(macrosNamePos, tok))
        }

        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MacroReplacement

        if (name != other.name) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}

class MacroFunction(name: String, internal val argNames: CTokenList, internal val value: TokenList): Macros(name) {
    override fun first(): CToken {
        return value.first() as CToken
    }

    override fun tokenString(): String {
        return "#define $name(${argNames.joinToString(", ") { it.str() }}) ${value.joinToString(" ") { it.str() }}"
    }
}

class SubstituteFunction(private val macros: MacroFunction, private val ctx: PreprocessorContext) {
    private fun seekNonSpace(idx: AnyToken?): CToken {
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
        val value = argToValue[i] ?: tokenListOf(i.cloneWith(current.position()))

        val preprocessed = CProgramPreprocessor.create(value, ctx).preprocess()

        val arg1 = result.findLast { it is CToken }
        if (arg1 == null) {
            return i
        }

        val str = preprocessed.joinToString("") { it.str() }
        val str1 = arg1.str()
        result.remove(arg1)

        if (result.lastOrNull() is Indent) {
            result.removeLast()
        }
        val tokens = CTokenizer.apply(str1 + str)
        result.addAll(tokens)
        return i.next()
    }

    private fun stringify(result: TokenList, argToValue: Map<CToken, TokenList>, current: CToken): AnyToken? {
        val value = argToValue[current] ?: throw MacroExpansionException("Invalid macro expansion: # without argument")
        val str = value.joinToString("") { it.str() }
        result.add(StringLiteral(str, current.position()))
        return current.next()
    }

    private fun evaluateSubstitution(args: List<TokenList>): Map<CToken, TokenList> {
        val res = mutableMapOf<CToken, TokenList>()
        macros.argNames.forEachWith(args) { arg, value ->
            res[arg] = value
        }
        return res
    }

    fun substitute(macrosNamePos: Position, args: List<TokenList>): TokenList {
        if (macros.name == "__MATH_PRECNAME") {
            if (args.size >= 2 && args[0].find { it.str() == "lgamma" } != null && args[1].find { it.str() == "_r" } != null) {
                println()
            }
        }
        val argToValue = evaluateSubstitution(args)

        val result = TokenList()
        var current = macros.value.firstOrNull()
        do {
            if (current == null) {
                break
            }
            if (current !is CToken) {
                result.add(current.cloneWith(PreprocessedPosition.UNKNOWN))
                current = current.next()
                continue
            }

            when(current.str()) {
                "##" -> {
                    current = concatTokens(result, argToValue, current)
                    continue
                }
                "#" -> {
                    current = seekNonSpace(current.next())

                    if (current.str() == "__VA_ARGS__") {
                        val stringBuilder = StringBuilder()
                        for (arg in args) {
                            for (tok in arg) {
                                stringBuilder.append(tok.str())
                            }
                            if (arg != args.last()) {
                                stringBuilder.append(", ")
                            }
                        }
                        result.add(StringLiteral(stringBuilder.toString(), current.position()))
                        current = current.next()
                        continue
                    } else {
                        current = stringify(result, argToValue, current)
                    }
                    continue
                }
                "__VA_ARGS__" -> {
                    val remains = args.takeLast(args.size - macros.argNames.size + 1)
                    for (arg in remains) {
                        for (tok in arg) {
                            result.add(tok.cloneWith(PreprocessedPosition.UNKNOWN))
                        }
                        if (arg != remains.last()) {
                            result.add(Punct(',', PreprocessedPosition.UNKNOWN))
                        }
                    }
                    current = current.next()
                    continue
                }
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