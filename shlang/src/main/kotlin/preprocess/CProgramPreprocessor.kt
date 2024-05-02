package preprocess

import common.AnyParser
import gen.consteval.ConstEvalExpression
import parser.CProgramParser
import tokenizer.*


data class PreprocessorException(override val message: String): Exception(message)


class CProgramPreprocessor(original: TokenIterator, private val ctx: PreprocessorContext): AnyParser(original.tokens()) {
    private fun kill() = killAt(current)

    private fun killAt(index: Int) = tokens.removeAt(index)

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

    private fun evaluateCondition(tokens: MutableList<CToken>): Int {
        val parser = CProgramParser.build(tokens)
        val constexpr = parser.constant_expression() ?:
            throw PreprocessorException("Cannot parse expression: '${TokenPrinter.print(tokens)}'")

        val evaluationContext = ConditionEvaluationContext(ctx)
        return ConstEvalExpression.eval(constexpr, evaluationContext)
    }

    private fun skipBlock() {
        var depth = 1
        while (!eof()) {
            if (check<NewLine>()) {
                eat()
                continue
            }
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
                return
            }
            if (check("endif") && depth == 1) {
                kill()
                return
            }
            if (check("elif") && depth == 1) {
                kill()
                val expr = takeCTokensInLine()
                val result = evaluateCondition(expr)
                if (result == 1) {
                    return
                }
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

    private fun parseDefineBody(): MutableList<AnyToken> {
        val value = mutableListOf<AnyToken>()
        while (!eof() && !check<NewLine>()) {
            value.add(peak())
            kill()
        }
        return value
    }

    private fun takeCTokensInLine(): MutableList<CToken> {
        val value = mutableListOf<CToken>()
        while (!eof() && !check<NewLine>()) {
            val peak = peak<AnyToken>();
            if (peak is CToken) {
                value.add(peak)
            }

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

    private fun checkNewLine() {
        if (eof()) {
            return
        }
        if (check<NewLine>()) {
            eat()
            return
        }
        throw PreprocessorException("Expected newline: '${peak<AnyToken>()}'")
    }

    // 6.10 Preprocessing directives
    private fun handleDirective() {
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
                    return
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
                checkNewLine()
                val macros = ctx.findMacros(name.str())
                if (macros != null) {
                    return
                }
                skipBlock()
            }
            "if" -> {
                killWithSpaces()
                val expr   = takeCTokensInLine()
                checkNewLine()
                val result = evaluateCondition(expr)
                if (result == 0) {
                    skipBlock()
                }
            }
            "elif" -> {
                skipBlock()
            }
            "ifndef" -> {
                killWithSpaces()
                if (!check<Ident>()) {
                    throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                }
                val name = peak<Ident>()
                kill()
                ctx.findMacros(name.str()) ?: return
                skipBlock()
            }
            "endif" -> {}
            "else" -> skipBlock()

            else -> throw PreprocessorException("Unknown directive '${directive.str()}'")
        }
    }

    private fun handleToken(tok: CToken) {
        val macros = ctx.findMacroReplacement(tok.str())
        if (macros != null) {
            kill()
            val replacement = macros.cloneContentWith(tok.position())
            addAll(replacement)
            if (macros.first().str() == tok.str()) {
                eat()
            }
            return
        }

        val macroFunction = ctx.findMacroFunction(tok.str())
        if (macroFunction != null) {
            macroFunctionReplacement(tok, macroFunction)
            return
        }

        val predefinedMacros = ctx.findPredefinedMacros(tok.str())
        if (predefinedMacros != null) {
            kill()
            val replacement = predefinedMacros.cloneContentWith(tok.position())
            add(replacement)
            if (replacement.str() == tok.str()) {
                eat()
            }
            return
        }

        eat()
    }

    private fun trimSpacesAtEnding() {
        var end = tokens.size - 1
        do {
            val last = tokens[end - 1]
            if (last is Indent || last is NewLine) {
                killAt(end - 1)
                end -= 1
                continue
            }
            break
        } while (true)
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
                handleToken(tok)
                continue
            }
            kill()
            handleDirective()
        }
        trimSpacesAtEnding()
        return tokens
    }

    companion object {
        fun create(tokens: TokenIterator, ctx: PreprocessorContext): CProgramPreprocessor {
            return CProgramPreprocessor(tokens, ctx)
        }
    }
}