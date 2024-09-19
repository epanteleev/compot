package preprocess

import tokenizer.*
import common.forEachWith
import preprocess.Macros.Companion.newTokenFrom


class SubstituteMacroFunction(private val macros: MacroFunction, private val ctx: PreprocessorContext) {
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