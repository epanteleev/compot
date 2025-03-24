package preprocess

import tokenizer.*
import tokenizer.tokens.*
import preprocess.macros.*


private enum class ConditionType {
    IF, ELIF, ELSE
}

private class ConditionState(var type: ConditionType, var isIncluded: Boolean)

class CProgramPreprocessor(filename: String, original: TokenList, private val ctx: PreprocessorContext): AbstractCPreprocessor(filename, original) {
    private val conditions = mutableListOf<ConditionState>()

    private fun enterCondition(type: ConditionType, condResult: Boolean) {
        conditions.add(ConditionState(type, condResult))
    }

    private fun exitCondition() {
        conditions.removeLast()
    }

    private fun evaluateCondition(tokens: TokenList): Long {
        return ConditionPreprocessor(filename, tokens, ctx)
            .evaluateCondition()
    }

    private fun skipBlock2() {
        var depth = 0
        while (!eof()) {
            if (check<NewLine>()) {
                eat()
                continue
            }
            if (check("#") && checkNextMacro("if", "ifdef", "ifndef")) {
                killWithSpaces()
                kill()
                depth += 1
                continue
            }
            if (check("#") && checkNextMacro("endif")) {
                depth -= 1
                killWithSpaces()
                kill()
                if (depth == 0) {
                    return
                }
            }
            killWithSpaces()
        }
        throw PreprocessorException("Cannot find #endif")
    }

    private fun skipConditionBlock() {
        while (!eof()) {
            if (check<NewLine>()) {
                eat()
                continue
            }
            if (check("#") && checkNextMacro("if", "ifdef", "ifndef")) {
                skipBlock2()
                continue
            }
            if (check("#") && checkNextMacro("endif", "else", "elif")) {
                return
            }
            killWithSpaces()
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
        killSpaces()
        while (!eof() && !check<NewLine>()) {
            value.add(kill())
        }
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
        val macroFunction = MacroFunction(name.str(), args, value)
        val old = ctx.define(macroFunction)
        warnRedefinedMacros(old, macroFunction)
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
                val name = peak<CToken>()
                kill()
                if (check("(")) {
                    killSpaces()
                    parseMacroFunction(name)
                    return
                }
                val macros = takeTokensInLine()
                if (macros.isEmpty()) {
                    ctx.define(MacroDefinition(name.str()))
                    return
                }
                val macroReplacement = MacroReplacement(name.str(), macros)
                val old = ctx.define(macroReplacement)
                warnRedefinedMacros(old, macroReplacement)
            }
            "undef" -> {
                if (!check<CToken>()) {
                    throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                }
                val macrosName = peak<CToken>()
                killWithSpaces()
                ctx.undef(macrosName.str())
            }
            "include" -> {
                ctx.enterInclude()
                handleToken(peak())
                val nameOrBracket = peak<AnyToken>()
                kill()
                if (nameOrBracket is StringLiteral) {
                    val header = ctx.findHeader(nameOrBracket.data(), HeaderType.USER) ?:
                        throw PreprocessorException("Cannot find header $nameOrBracket")

                    val includeTokens = preprocessHeader(header, nameOrBracket.line(), ctx)
                    addInclude(includeTokens)

                } else if (nameOrBracket.str() == "<") {
                    nameOrBracket as CToken
                    if (!check<CToken>()) {
                        throw PreprocessorException("Expected file name: but '${peak<AnyToken>()}'")
                    }
                    val headerName = readHeaderName()
                    val header = ctx.findHeader(headerName, HeaderType.SYSTEM) ?:
                        throw PreprocessorException("Cannot find system header '$headerName'", nameOrBracket.position())

                    val includeTokens = preprocessHeader(header, nameOrBracket.line(), ctx)
                    addInclude(includeTokens)

                } else {
                    throw PreprocessorException("Expected string literal or '<': but '${nameOrBracket}'")
                }
                ctx.exitInclude()
            }
            "ifdef" -> {
                if (!check<CToken>()) {
                    throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
                }
                val name = peak<CToken>()
                killWithSpaces()
                checkNewLine()
                val macros = ctx.findMacros(name.str())
                enterCondition(ConditionType.IF, macros != null)
                if (macros == null) {
                    skipConditionBlock()
                }
            }
            "if" -> {
                val constExpression = takeTokensInLine()
                val result = evaluateCondition(constExpression)
                enterCondition(ConditionType.IF, result != 0L)
                if (result == 0L) {
                    skipConditionBlock()
                }
            }
            "elif" -> {
                if (conditions.isEmpty()) {
                    throw PreprocessorException("Unexpected #elif")
                }
                val last = conditions.last()
                if (last.type == ConditionType.ELSE) {
                    throw PreprocessorException("Unexpected #elif after #else")
                }
                last.type = ConditionType.ELIF
                if (last.isIncluded) {
                    skipConditionBlock()
                } else {
                    val constExpression = takeTokensInLine()
                    val result = evaluateCondition(constExpression)
                    last.isIncluded = result != 0L
                    if (result == 0L) {
                        skipConditionBlock()
                    }
                }
            }
            "ifndef" -> {
                if (!check<CToken>()) {
                    throw PreprocessorException("Expected identifier: but '${peak<CToken>()}'")
                }
                val macros = peak<CToken>()
                killWithSpaces()
                checkNewLine()
                val hasDef = ctx.findMacros(macros.str()) != null
                enterCondition(ConditionType.IF, !hasDef)
                if (hasDef) {
                    skipConditionBlock()
                }
            }
            "error" -> {
                val message = takeTokensInLine()
                throw PreprocessorException("#error ${TokenPrinter.print(message)}", directive.position())
            }
            "pragma" -> {
                val name = takeTokensInLine()
                if (name.isEmpty()) {
                    throw PreprocessorException("Expected pragma name", directive.position())
                }
                checkNewLine()

                val first = name.first().asToken<CToken>()
                when (first.str()) {
                    "once" -> ctx.addPragmaOnce(filename)
                    else -> warning("Unknown pragma: ${TokenPrinter.print(name)}", first.position())
                }
            }
            "endif" -> {
                if (conditions.isEmpty()) {
                    throw PreprocessorException("Unexpected #endif")
                }
                exitCondition()
            }
            "else" -> {
                if (conditions.isEmpty()) {
                    throw PreprocessorException("Unexpected #else")
                }
                val last = conditions.last()
                if (last.type == ConditionType.ELSE) {
                    throw PreprocessorException("Unexpected #else after #else")
                }
                last.type = ConditionType.ELSE
                if (last.isIncluded) {
                    skipConditionBlock()
                }
            }
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

    private fun getMacroReplacement(macros: Macros, tok: CToken): TokenList? {
        when (macros) {
            is MacroReplacement -> {
                return macros.substitute(tok.position())
            }
            is PredefinedMacros -> {
                return macros.cloneContentWith(tok.position())
            }
            is MacroFunction -> {
                killNewLines()
                val spaces = killSpaces()
                if (!check("(")) {
                    add(tok)
                    if (spaces != 0) {
                        add(Indent.of(spaces))
                    }
                    return null
                }
                killWithSpaces()
                val args = parseMacroFunctionArguments()

                return SubstituteMacroFunction(macros, ctx, args)
                    .substitute(tok.position())
            }
            is MacroDefinition -> return TokenList()
        }
    }

    private fun handleToken(tok: CToken): Boolean {
        val macros = ctx.findMacros(tok.str()) ?: return false
        if (tok.hideset.contains(macros.name)) {
            return false
        }
        kill()
        val replacement = getMacroReplacement(macros, tok) ?: return false
        if (replacement.isEmpty()) {
            return true
        }
        addAll(replacement)

        return true
    }

    private fun preprocess0(): TokenList {
        while (!eof()) {
            if (!check<CToken>()) {
                eat()
                continue
            }
            if (!check("#")) {
                val tok = peak<CToken>()
                if (!handleToken(tok)) {
                    eat()
                }
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

    private fun warnRedefinedMacros(old: Macros?, new: Macros) {
        if (old != null && old != new) {
            warning("macro ${new.name} already defined", old.first().position())
        }
    }

    companion object {
        fun create(filename: String, tokens: TokenList, ctx: PreprocessorContext): CProgramPreprocessor {
            return CProgramPreprocessor(filename, tokens, ctx)
        }

        private fun preprocessHeader(header: Header, line: Int, ctx: PreprocessorContext): TokenList {
            if (ctx.isPragmaOnce(header.filename)) {
                return TokenList()
            }

            val includeTokens = create(header.filename, header.tokenize(), ctx).preprocess()
            includeTokens.addBefore(null, EnterIncludeGuard(header.filename, ctx.includeLevel(), line))
            includeTokens.addAfter(null, ExitIncludeGuard(header.filename, ctx.includeLevel(), line))
            return includeTokens
        }
    }
}