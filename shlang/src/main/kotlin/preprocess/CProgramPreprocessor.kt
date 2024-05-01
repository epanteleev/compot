package preprocess

import common.AnyParser
import tokenizer.*

data class PreprocessorException(override val message: String): Exception(message)


class CProgramPreprocessor(original: TokenIterator, private val ctx: PreprocessorContext): AnyParser(original.tokens()) {
    private fun kill() {
        tokens.removeAt(current)
    }

    private fun killWithSpaces() {
        kill()
        while (!eof() && check<Indent>()) {
            kill()
        }
    }

    private fun addAll(others: List<AnyToken>) {
        tokens.addAll(current, others)
    }

    private fun add(tok: AnyToken) {
        tokens.add(current, tok)
    }

    private fun skipBlock(): Boolean {
        var depth = 1
        while (!eof()) {
            if (!check("#")) {
                kill()
                continue
            }
            kill()
            if (check("ifdef") || check("ifndef")) {
                kill()
                depth += 1
            }
            if (check("else") && depth == 1) {
                kill()
                return true
            }
            if (check("endif") && depth == 1) {
                kill()
                return false
            }
        }
        throw PreprocessorException("Cannot find #endif")
    }

    private fun parseArguments(): List<List<CToken>> {
        val args = mutableListOf<MutableList<CToken>>(mutableListOf())
        var depth = 1
        while (depth != 0) {
            if (check("(")) {
                depth += 1
            }
            if (check(")")) {
                depth -= 1
                if (depth == 0) {
                    break
                }
            }
            if (check(",")) {
                args.add(mutableListOf())
                killWithSpaces()
                continue
            }
            args.last().add(peak<CToken>())
            killWithSpaces()
        }
        kill()
        return args
    }

    private fun macroFunctionReplacement(name: CToken, macroFunction: MacroFunction): Boolean {
        killWithSpaces()
        if (!check("(")) {
            add(name)
            eat()
            return false
        }
        killWithSpaces()
        val args = parseArguments()
        val replacement = macroFunction.cloneContentWith(name.position(), args)
        addAll(replacement)
        if (macroFunction.first().str() == name.str()) {
            eat()
        }

        return true
    }

    private fun parseDefineBody(): List<AnyToken> {
        val value = mutableListOf<AnyToken>()
        while (!check<NewLine>()) {
            value.add(peak())
            kill()
        }
        return value
    }

    private fun parseArgumentDefinition(): List<CToken> {
        val args = mutableListOf<CToken>()

        var depth = 1
        while (depth != 0) {
            if (check("(")) {
                depth += 1
            }
            if (check(")")) {
                depth -= 1
                if (depth == 0) {
                    break
                }
            }
            if (check(",")) {
                killWithSpaces()
                continue
            }
            args.add(peak<CToken>())
            killWithSpaces()
        }
        killWithSpaces()
        return args
    }

    private fun parseMacroFunction(name: CToken) {
        killWithSpaces()
        val args = parseArgumentDefinition()
        val value = parseDefineBody()
        ctx.define(MacroFunction(name.str(), args, value))
    }

    fun preprocess(): List<AnyToken> {
        while (!eof()) {
            if (check<NewLine>()) {
                eat()
                continue
            }
            if (check<Indent>()) {
                eat()
                continue
            }
            if (!check("#")) {
                val tok = peak<CToken>()
                val macros = ctx.findMacroReplacement(tok.str())
                if (macros != null) {
                    kill()
                    val replacement = macros.cloneContentWith(tok.position())
                    addAll(replacement)
                    if (macros.first().str() == tok.str()) {
                        eat()
                    }
                    continue
                }

                val macroFunction = ctx.findMacroFunction(tok.str())
                if (macroFunction != null) {
                    macroFunctionReplacement(tok, macroFunction)
                    continue
                }
                eat()
                continue
            }
            kill()
            if (!check<CToken>()) {
                throw PreprocessorException("Expected directive: '${peak<AnyToken>()}'")
            }

            val directive = peak<CToken>()
            kill()
            when (directive.str()) {
                "define" -> {
                    killWithSpaces()
                    val name = peak<Ident>()
                    killWithSpaces()
                    if (check("(")) {
                        parseMacroFunction(name)
                        continue
                    }

                    val value = parseDefineBody()
                    if (value.isEmpty()) {
                        ctx.define(MacroDefinition(name.str()))
                    } else {
                        ctx.define(MacroReplacement(name.str(), value))
                    }
                }
                "undef" -> {
                    killWithSpaces()
                    if (!check<Ident>()) {
                        throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                    }
                    val name = peak<Ident>()
                    killWithSpaces()
                    ctx.undef(name.str())
                }
                "include" -> {
                    killWithSpaces()
                    if (!check<StringLiteral>()) {
                        throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                    }
                    val name = peak<StringLiteral>()
                    kill()
                    val header = ctx.findHeader(name.unquote()) ?: throw PreprocessorException("Cannot find header '$name'")
                    val tokens = header.tokenize()
                    val preprocessor = CProgramPreprocessor(tokens, ctx)
                    addAll(preprocessor.preprocess())
                }
                "ifdef" -> {
                    killWithSpaces()
                    if (!check<Ident>()) {
                        throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                    }
                    val name = peak<Ident>()
                    kill()
                    val macros = ctx.findMacros(name.str())
                    if (macros != null) {
                        continue
                    }
                    skipBlock()
                }
                "ifndef" -> {
                    killWithSpaces()
                    if (!check<Ident>()) {
                        throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                    }
                    val name = peak<Ident>()
                    kill()
                    ctx.findMacros(name.str()) ?: continue
                    skipBlock()
                }
                "endif" -> {}
                "else" -> skipBlock()

                else -> throw PreprocessorException("Unknown directive ${directive.str()}")
            }
        }
        return tokens
    }

    companion object {
        fun create(tokens: TokenIterator, ctx: PreprocessorContext): CProgramPreprocessor {
            return CProgramPreprocessor(tokens, ctx)
        }
    }
}