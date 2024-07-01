package preprocess

import tokenizer.*
import parser.CProgramParser
import gen.consteval.ConstEvalExpression
import gen.consteval.ConstEvalExpressionInt


class CProgramPreprocessor(original: TokenList, private val ctx: PreprocessorContext): AbstractCPreprocessor(original) {
    private fun preprocessCondition(tokens: TokenList): TokenList {
        var token: AnyToken? = tokens.first()
        while (token != null) {
            if (token.str() != "defined") {
                token = token.next()
                continue
            }
            token = tokens.kill(token)

            if (token !is CToken) {
                throw PreprocessorException("Expected identifier: but '${token}'")
            }
            val hasParen = token.str() == "("
            if (hasParen) {
                token = token.next()
            }
            val name = token as CToken
            val macros = ctx.findMacros(name.str())

            if (macros == null) {
                val num = Numeric(0, name.position())
                tokens.addAfter(name, num)
                tokens.kill(name)
                token = num.next()
            } else {
                val num = Numeric(1, name.position())
                tokens.addAfter(name, num)
                tokens.kill(name)
                token = num.next()
            }

            if (hasParen) {
                if (token !is CToken || token.str() != ")") {
                    throw PreprocessorException("Expected ')': but '${token}'")
                }
                token = token.next()
            }
        }
        return create(tokens, ctx).preprocess()
    }

    private fun evaluateCondition(tokens: TokenList): Int {
        val preprocessed = preprocessCondition(tokens)

        val parser = CProgramParser.build(preprocessed)
        val constexpr = parser.constant_expression() ?:
            throw PreprocessorException("Cannot parse expression: '${TokenPrinter.print(preprocessed)}'")

        val evaluationContext = ConditionEvaluationContext(ctx)
        return ConstEvalExpression.eval(constexpr, ConstEvalExpressionInt(evaluationContext))
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
            killWithSpaces()
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
                val result = evaluateCondition(expr)
                if (result == 1) {
                    return
                }
            }
        }
        throw PreprocessorException("Cannot find #endif")
    }

    private fun parseMacroFunctionArguments(): List<TokenList> {
        val args = mutableListOf(TokenList())
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
                args.add(TokenList())
                killWithSpaces()
                continue
            }
            val arg = peak<AnyToken>()
            kill()
            args.last().add(arg)
        }
        kill()
        return args
    }

    private fun takeTokensInLine(): TokenList {
        val value = TokenList()
        while (!eof() && !check<NewLine>()) {
            value.add(kill())
        }
        return value
    }

    private fun takeCTokensInLine(): TokenList {
        val value = TokenList()
        while (!eof() && !check<NewLine>()) {
            val peak = peak<AnyToken>()
            kill()
            if (peak is CToken) {
                value.add(peak)
            }
        }
        checkNewLine()
        return value
    }

    private fun parseArgumentDefinition(): CTokenList {
        val args = CTokenList()

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
            val arg = peak<CToken>()
            killWithSpaces()
            args.add(arg)
        }
        killWithSpaces()
        return args
    }

    private fun parseMacroFunction(name: CToken) {
        killWithSpaces()
        val args = parseArgumentDefinition()
        val value = takeTokensInLine()
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
        kill()
        return tokens.joinToString("") { it.str() }
    }

    private fun restoreSharp(removedSpaces: Int, sharp: AnyToken) {
        add(sharp)
        if (removedSpaces != 0) {
            add(Indent.of(removedSpaces))
        }
    }

    // 6.10 Preprocessing directives
    private fun handleDirective(removedSpaces: Int, sharp: AnyToken) {
        if (!check<CToken>()) {
            restoreSharp(removedSpaces, sharp)
            eat()
            return
        }

        val directive = peak<CToken>()
        kill()
        val spaces = killSpaces()
        when (directive.str()) {
            "define" -> {
                val name = peak<Identifier>()
                killWithSpaces()
                if (check("(")) {
                    parseMacroFunction(name)
                    return
                }

                val value = takeTokensInLine()
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
                ctx.enterInclude()
                val nameOrBracket = includeName()
                if (nameOrBracket is StringLiteral) {
                    val header = ctx.findHeader(nameOrBracket.unquote(), HeaderType.USER) ?:
                        throw PreprocessorException("Cannot find header $nameOrBracket")

                    val result = create(header.tokenize(), ctx).preprocess()
                    result.addBefore(null, EnterIncludeGuard(nameOrBracket.unquote(), ctx.includeLevel()))
                    result.addAfter(null, ExitIncludeGuard(nameOrBracket.unquote(), ctx.includeLevel()))
                    addAll(result)

                } else if (nameOrBracket.str() == "<") {
                    nameOrBracket as CToken
                    if (!check<Identifier>()) {
                        throw PreprocessorException("Expected file name: but '${peak<AnyToken>()}'")
                    }
                    val name = readHeaderName()
                    val header = ctx.findHeader(name, HeaderType.SYSTEM) ?:
                        throw PreprocessorException("Cannot find system header '$name'", nameOrBracket.position())

                    val result = create(header.tokenize(), ctx).preprocess()
                    result.addBefore(null, EnterIncludeGuard(name, ctx.includeLevel()))
                    result.addAfter(null, ExitIncludeGuard(name, ctx.includeLevel()))
                    addAll(result)
                } else {
                    throw PreprocessorException("Expected string literal or '<': but '${nameOrBracket}'")
                }
                ctx.exitInclude()
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
                checkNewLine()
                ctx.findMacros(name.str()) ?: return
                skipBlock()
            }
            "error" -> {
                val message = takeTokensInLine()
                throw PreprocessorException("#error ${TokenPrinter.print(message)}", directive.position())
            }
            "pragma" -> {
                TODO("Implement #pragma directive")
            }
            "endif" -> {}
            "else" -> skipBlock()

            else -> {
                restoreSharp(removedSpaces, sharp)
                add(directive)
                if (spaces != 0) {
                    add(Indent.of(spaces))
                }
                eat()
            }
        }
    }

    private fun includeName(): AnyToken {
        val nameOrBracket = peak<AnyToken>()
        if (nameOrBracket is Identifier && ctx.findMacros(nameOrBracket.str()) != null) {
            val (_, replacement) = getMacroReplacement(nameOrBracket)
            return replacement.first() //Ignore remains
        } else {
            kill()
            return nameOrBracket
        }
    }

    private fun getMacroReplacement(tok: CToken): Pair<Macros?, TokenList> {
        val macros = ctx.findMacroReplacement(tok.str())
        if (macros != null) {
            kill()
            return Pair(macros, macros.substitute(tok.position()))
        }

        val macroFunction = ctx.findMacroFunction(tok.str())
        if (macroFunction != null) {
            killWithSpaces()
            if (!check("(")) {
                add(tok)
                return Pair(null, TokenList())
            }
            killWithSpaces()
            val args = parseMacroFunctionArguments()
            return Pair(macroFunction, macroFunction.substitute(tok.position(), args))
        }

        val predefinedMacros = ctx.findPredefinedMacros(tok.str())
        if (predefinedMacros != null) {
            kill()
            val clone = predefinedMacros.cloneContentWith(tok.position())
            return Pair(predefinedMacros, clone)
        }

        val macro = ctx.findMacroDefinition(tok.str())
        if (macro != null) {
            kill()
            return Pair(macro, TokenList())
        }
        return Pair(null, TokenList())
    }

    private fun handleToken(tok: CToken) {
        val (macros, replacement) = getMacroReplacement(tok)
        if (macros == null) {
            eat()
            return
        }
        if (replacement.isEmpty()) {
            return
        }
        addAll(replacement)
        if (macros.first().str() == tok.str()) {
            eat()
        }
    }

    private fun preprocess0(): TokenList {
        while (!eof()) {
            if (!check<CToken>()) {
                eat()
                continue
            }
            if (!check("#")) {
                val tok = peak<CToken>()
                handleToken(tok)
                continue
            }
            val sharp = kill()
            val spaces = killSpaces()
            handleDirective(spaces, sharp)
        }
        return tokens
    }

    fun preprocess(): TokenList {
        preprocess0()
        trimSpacesAtEnding()
        return tokens
    }

    companion object {
        fun create(tokens: TokenList, ctx: PreprocessorContext): CProgramPreprocessor {
            return CProgramPreprocessor(tokens, ctx)
        }
    }
}