package preprocess

import common.AnyParser
import gen.consteval.ConstEvalExpression
import parser.CProgramParser
import tokenizer.*


data class PreprocessorException(override val message: String): Exception(message)


class CProgramPreprocessor(original: TokenIterator, private val ctx: PreprocessorContext): AnyParser(original.tokens()) {
    private fun kill(): AnyToken = killAt(current)

    private fun killAt(index: Int): AnyToken = tokens.removeAt(index)

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
            if (check("if") || check("ifdef") || check("ifndef")) { //TODO reactor conditions
                kill()
                depth += 1
            }
            if (check("endif") || check("else")) {
                kill()
                depth -= 1
                if (depth == 0) {
                    return
                }
            }
            if (check("elif")) {
                kill()
                if (depth != 1) {
                    continue
                }
                val expr = takeCTokensInLine()
                checkNewLine()
                val result = evaluateCondition(expr)
                if (result == 1) {
                    return
                }
            }
        }
        throw PreprocessorException("Cannot find #endif")
    }

    private fun parseArguments(): List<List<AnyToken>> {
        val args = mutableListOf<MutableList<AnyToken>>(mutableListOf())
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
            if (check(",") && depth == 1) {
                args.add(mutableListOf())
                killWithSpaces()
                continue
            }
            args.last().add(peak())
            kill()
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

    private fun takeAntTokensInLine(): MutableList<AnyToken> {
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
        val value = takeAntTokensInLine()
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

    private fun readHeaderName(): String {
        val tokens = mutableListOf<CToken>()
        while (!eof() && !check<NewLine>()) {
            val token = peak<CToken>()
            if (token.str() == ">") {
                break
            }
            tokens.add(token)
            kill()
        }
        if (peak<AnyToken>().str() != ">") {
            throw PreprocessorException("Expected '>': '${peak<AnyToken>()}'")
        }
        return tokens.joinToString("") { it.str() }
    }

    // 6.10 Preprocessing directives
    private fun handleDirective(sharp: CToken) {
        if (!check<CToken>()) {
            throw PreprocessorException("Expected directive: '${peak<AnyToken>()}'")
        }

        val directive = peak<CToken>()
        killWithSpaces()
        when (directive.str()) {
            "define" -> {
                val name = peak<Identifier>()
                killWithSpaces()
                if (check("(")) {
                    parseMacroFunction(name)
                    return
                }

                val value = takeAntTokensInLine()
                if (value.isEmpty()) {
                    ctx.define(MacroDefinition(name.str()))
                } else {
                    ctx.define(MacroReplacement(name.str(), value))
                }
            }
            "undef" -> {
                if (!check<Identifier>()) {
                    throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                }
                val name = peak<Identifier>()
                killWithSpaces()
                ctx.undef(name.str())
            }
            "include" -> {
                val nameOrBracket = peak<AnyToken>()
                if (nameOrBracket is StringLiteral) {
                    kill()
                    val header = ctx.findHeader(nameOrBracket.unquote(), HeaderType.USER) ?:
                        throw PreprocessorException("Cannot find header '$nameOrBracket'")

                    val includeTokens = header.tokenize()
                    val preprocessor = CProgramPreprocessor(includeTokens, ctx)
                    val result = preprocessor.preprocess0() //TODO refactor
                    result.removeLast()
                    addAll(result)
                    return

                } else if (nameOrBracket.str() == "<") {
                    kill()
                    if (!check<Identifier>()) {
                        throw PreprocessorException("Expected file name: but '${peak<AnyToken>()}'")
                    }
                    val name = readHeaderName()
                    kill()
                    val header = ctx.findHeader(name, HeaderType.SYSTEM) ?:
                        throw PreprocessorException("Cannot find system header '$name'")

                    val tokens = header.tokenize()
                    val preprocessor = CProgramPreprocessor(tokens, ctx)
                    val result = preprocessor.preprocess0() //TODO refactor
                    result.removeLast()
                    addAll(result)
                    return

                } else if (nameOrBracket is Identifier && ctx.findMacros(nameOrBracket.str()) != null) {
                    val start = current //TODO unsafe
                    add(Indent.of(1))
                    add(directive)
                    add(sharp)
                    current += 3
                    handleToken(nameOrBracket)
                    current = start
                }  else {
                    throw PreprocessorException("Expected string literal or '<': but '${nameOrBracket}'")
                }
            }
            "ifdef" -> {
                if (!check<Identifier>()) {
                    throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                }
                val name = peak<Identifier>()
                kill()
                checkNewLine()
                val macros = ctx.findMacros(name.str())
                if (macros != null) {
                    return
                }
                skipBlock()
            }
            "if" -> {
                val expr = takeCTokensInLine()
                checkNewLine()
                val result = evaluateCondition(expr)
                if (result == 0) {
                    skipBlock()
                }
            }
            "elif" -> skipBlock()
            "ifndef" -> {
                if (!check<Identifier>()) {
                    throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                }
                val name = peak<Identifier>()
                kill()
                ctx.findMacros(name.str()) ?: return
                skipBlock()
            }
            "error" -> {
                val message = takeAntTokensInLine()
                throw PreprocessorException("#error ${TokenPrinter.print(message)}")
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
            val last = tokens[end - 1] //TODO unsafe API
            if (last is Indent || last is NewLine) {
                killAt(end - 1)
                end -= 1
                continue
            }
            break
        } while (true)
    }

    private fun preprocess0(): MutableList<AnyToken> {
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
            val sharp = kill() as CToken
            handleDirective(sharp)
        }
        return tokens
    }

    fun preprocess(): List<AnyToken> {
        preprocess0()
        trimSpacesAtEnding()
        return tokens
    }

    fun preprocessWithKilledSpaces(): MutableList<CToken> {
        val tokens = preprocess0()
        tokens.retainAll { it !is Indent && it !is NewLine }
        return tokens as MutableList<CToken> //TODO
    }

    companion object {
        fun create(tokens: TokenIterator, ctx: PreprocessorContext): CProgramPreprocessor {
            return CProgramPreprocessor(tokens, ctx)
        }
    }
}