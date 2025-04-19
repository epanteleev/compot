package preprocess

import tokenizer.*
import common.forEachWith
import preprocess.CProgramPreprocessor.Companion.create
import preprocess.macros.MacroExpansionException
import preprocess.macros.MacroFunction
import tokenizer.tokens.*
import preprocess.macros.Macros.Companion.newTokenFrom


internal class SubstituteMacroFunction(private val macros: MacroFunction, private val ctx: PreprocessorContext, private val args: List<TokenList>):
    AbstractCPreprocessor(macros.name, macros.value) {
    private val result = TokenList()
    private val argToValue = evaluateSubstitution(args)

    private fun evaluateSubstitution(args: List<TokenList>): Map<CToken, TokenList> {
        val res = hashMapOf<CToken, TokenList>()
        macros.argNames.forEachWith(args) { arg, value ->
            res[arg] = value
        }
        return res
    }

    private fun concatTokens(where: Position, current: CToken) {
        val value = argToValue[current] ?: tokenListOf(current.cloneWith(current.position()))
        val arg1 = result.findLast { it is CToken }
        if (arg1 == null) {
            return
        }

        result.remove(arg1)
        if (result.lastOrNull() is Indent) {
            result.removeLast()
        }

        val str = value.joinToString("", prefix = arg1.str()) { it.str() }
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

    private fun concatVariadicArgs(): String = buildString {
        val remains = args.takeLast(args.size - macros.argNames.size + 1)
        for (arg in remains) {
            for (tok in arg) {
                append(tok.str())
            }
            if (arg != remains.last()) {
                append(", ")
            }
        }
    }

    private fun consumeVariadicArgs(macrosNamePos: Position): TokenList {
        val remains = args.takeLast(args.size - macros.argNames.size + 1)
        for (arg in remains) {
            for (tok in arg) {
                val copy = when (tok) {
                    is CToken -> tok.cloneWith(macrosNamePos)
                    else -> tok.copy()
                }
                result.add(copy)
            }
            if (arg != remains.last()) {
                result.add(Punctuator(",", macrosNamePos))
                result.add(Indent.of(1))
            }
        }
        eat()
        return result
    }

    fun substitute(macrosNamePos: Position): TokenList {
        while (!eof()) {
            val current = peak<AnyToken>()
            if (current !is CToken) {
                result.add(current.copy())
                eat()
                continue
            }

            if (check("##")) {
                eat()
                eatSpaces()
                if (!check("__VA_ARGS__")) {
                    concatTokens(macrosNamePos, peak())
                    continue
                }
                // ##__VA_ARGS__ pattern
                consumeVariadicArgs(macrosNamePos)
                continue
            }

            if (check("#")) {
                eat()
                eatSpaces()

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
                consumeVariadicArgs(macrosNamePos)
                continue
            }

            val value = argToValue[peak()]
            if (value == null) {
                result.add(newTokenFrom(macros.name, macrosNamePos, peak()))
                eat()
                continue
            }

            val preprocessed = create(filename, value.clone(), ctx).preprocess()
            val preprocessedPosition = PreprocessedPosition.makeFrom(macrosNamePos, peak<CToken>().position() as OriginalPosition)
            for (tok in preprocessed) {
                val copy = when (tok) {
                    is CToken -> tok.cloneWith(preprocessedPosition)
                    else -> tok.copy()
                }
                result.add(copy)
            }
            eat()
        }
        return result
    }
}