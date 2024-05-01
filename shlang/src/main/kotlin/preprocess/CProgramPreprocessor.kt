package preprocess

import common.AnyParser
import tokenizer.*

data class PreprocessorException(override val message: String): Exception(message)


class CProgramPreprocessor(original: TokenIterator, private val ctx: PreprocessorContext): AnyParser(original.tokens()) {

    private fun skipBlock(): Boolean {
        var depth = 1
        while (!eof()) {
            if (!check("#")) {
                eat()
                continue
            }
            eat()
            if (check("ifdef") || check("ifndef")) {
                eat()
                depth += 1
            }
            if (check("else") && depth == 1) {
                eat()
                return true
            }
            if (check("endif") && depth == 1) {
                eat()
                return false
            }
        }
        throw PreprocessorException("Cannot find #endif")
    }

    fun preprocess(): List<AnyToken> {
        val result = mutableListOf<AnyToken>()
        while (!eof()) {
            if (check<NewLine>()) {
                result.add(peak())
                eat()
                continue
            }
            if (check<Indent>()) {
                result.add(peak())
                eat()
                continue
            }
            if (!check("#")) {
                val tok = peak<CToken>()
                val macros = ctx.findMacroReplacement(tok.str())
                if (macros == null) {
                    result.add(peak())
                    eat()
                    continue
                }
                eat()
                result.addAll(macros.cloneContentWith(tok.position()))
                continue
            }
            eat()
            if (!check<CToken>()) {
                throw PreprocessorException("Expected directive: '${peak<AnyToken>()}'")
            }

            val directive = peak<CToken>()
            eat()
            when (directive.str()) {
                "define" -> {
                    eatWithSpaces()
                    val name = peak<Ident>()
                    eatWithSpaces()
                    val value = mutableListOf<AnyToken>()
                    while (!check<NewLine>()) {
                        value.add(peak())
                        eat()
                    }
                    if (value.isEmpty()) {
                        ctx.define(MacroDefinition(name.str()))
                    } else {
                        ctx.define(MacroReplacement(name.str(), value))
                    }
                }
                "undef" -> {
                    eatWithSpaces()
                    if (!check<Ident>()) {
                        throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                    }
                    val name = peak<Ident>()
                    eat()
                    ctx.undef(name.str())
                }
                "include" -> {
                    eatWithSpaces()
                    if (!check<StringLiteral>()) {
                        throw PreprocessorException("Expected identifier")
                    }
                    val name = peak<StringLiteral>()
                    eat()
                    val header = ctx.findHeader(name.unquote()) ?: throw PreprocessorException("Cannot find header '$name'")
                    val tokens = header.tokenize()
                    val preprocessor = CProgramPreprocessor(tokens, ctx)
                    result.addAll(preprocessor.preprocess())
                }
                "ifdef" -> {
                    eatWithSpaces()
                    if (!check<Ident>()) {
                        throw PreprocessorException("Expected identifier")
                    }
                    val name = peak<Ident>()
                    eat()
                    val macros = ctx.findMacros(name.str())
                    if (macros != null) {
                        continue
                    }
                    skipBlock()
                }
                "ifndef" -> {
                    eatWithSpaces()
                    if (!check<Ident>()) {
                        throw PreprocessorException("Expected identifier")
                    }
                    val name = peak<Ident>()
                    eat()
                    ctx.findMacros(name.str()) ?: continue
                    skipBlock()
                }
                "endif" -> {}
                "else" -> {
                    skipBlock()
                }

                else -> throw PreprocessorException("Unknown directive ${directive.str()}")
            }
        }
        return result
    }

    companion object {
        fun create(tokens: TokenIterator, ctx: PreprocessorContext): CProgramPreprocessor {
            return CProgramPreprocessor(tokens, ctx)
        }
    }
}