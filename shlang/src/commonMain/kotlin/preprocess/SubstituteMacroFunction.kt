package preprocess

import tokenizer.*
import common.forEachWith
import preprocess.macros.MacroExpansionException
import preprocess.macros.MacroFunction
import tokenizer.tokens.*
import preprocess.macros.Macros.Companion.newTokenFrom


class SubstituteMacroFunction(private val macros: MacroFunction, private val ctx: PreprocessorContext, val args: List<TokenList>):
    AbstractCPreprocessor(macros.name, macros.value) {
    private val result = TokenList()
    private val argToValue = evaluateSubstitution(args)

    private fun evaluateSubstitution(args: List<TokenList>): Map<CToken, TokenList> {
        val res = mutableMapOf<CToken, TokenList>()
        macros.argNames.forEachWith(args) { arg, value ->
            res[arg] = value
        }
        return res
    }

    private fun concatTokens(where: Position, current: CToken) {
        val value = argToValue[current] ?: tokenListOf(current.cloneWith(current.position()))
        val preprocessed = CProgramPreprocessor.create(where.filename(), value, ctx).preprocess()

        val arg1 = result.findLast { it is CToken }
        if (arg1 == null) {
            return
        }

        val str1 = arg1.str()
        result.remove(arg1)

        if (result.lastOrNull() is Indent) {
            result.removeLast()
        }

        val str = preprocessed.joinToString("", prefix = str1) { it.str() }
        val tokens = CTokenizer.apply(str, where.filename())
        result.addAll(tokens)
        eat()
    }

    private fun stringify(current: CToken) {
        val value = argToValue[current] ?: throw MacroExpansionException("Invalid macro expansion: # without argument")
        val builder = StringBuilder()
        for (tok in value) {
            builder.append(tok.str())
        }

        result.add(StringLiteral(builder.toString(), current.position()))
        eat()
    }

    private fun concatVariadicArgs(): String {
        val stringBuilder = StringBuilder()
        for (arg in args) {
            for (tok in arg) {
                stringBuilder.append(tok.str())
            }
            if (arg != args.last()) {
                stringBuilder.append(", ")
            }
        }

        return stringBuilder.toString()
    }

    fun substitute(macrosNamePos: Position): TokenList {
        while (!eof()) {
            val current = peak<AnyToken>()
            if (current !is CToken) {
                result.add(current.cloneWith(macrosNamePos))
                eat()
                continue
            }

            if (check("##")) {
                eat()
                eatSpace()
                concatTokens(macrosNamePos, peak())
                continue
            }

            if (check("#")) {
                eat()
                eatSpace()

                if (!check("__VA_ARGS__")) {
                    stringify(peak())
                    continue
                }
                // #__VA_ARGS__ pattern
                val concat = concatVariadicArgs()
                result.add(StringLiteral(concat, peak<CToken>().position()))
                eat()
                continue
            }

            if (check("__VA_ARGS__")) {
                val remains = args.takeLast(args.size - macros.argNames.size + 1)

                for (arg in remains) {
                    for (tok in arg) {
                        result.add(tok.cloneWith(macrosNamePos))
                    }
                    if (arg != remains.last()) {
                        result.add(Punctuator(",", macrosNamePos))
                    }
                }
                eat()
                continue
            }

            val value = argToValue[peak()]
            if (value == null) {
                result.add(newTokenFrom(macros.name, macrosNamePos, peak()))
                eat()
                continue
            }

            val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, peak<CToken>().position() as OriginalPosition)
            for (tok in value) {
                result.add(tok.cloneWith(preprocessedPosition))
            }
            eat()
        }
        return result
    }
}