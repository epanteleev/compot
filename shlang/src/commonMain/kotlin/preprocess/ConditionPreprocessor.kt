package preprocess

import tokenizer.*
import tokenizer.tokens.*
import parser.CProgramParser
import codegen.consteval.ConstEvalExpression
import codegen.consteval.TryConstEvalExpressionLong
import preprocess.CProgramPreprocessor.Companion.create


class ConditionPreprocessor(filename: String, condition: TokenList, val ctx: PreprocessorContext): AbstractCPreprocessor(filename, condition) {
    private fun preprocess(): TokenList {
        while (!eof()) {
            if (!check("defined")) {
                eat()
                continue
            }
            // 6.10.1 Conditional inclusion

            // Found pattern:
            //  | defined MACROS
            //  | defined (MACROS)
            kill()
            eatSpace()
            if (!check<CToken>()) {
                throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
            }
            val hasParen = check("(")
            if (hasParen) {
                kill()
            }
            val name = peak<CToken>()
            kill()

            val num = if (ctx.findMacros(name.str()) == null) "0" else "1"
            add(PPNumber(num, 10, name.position()))

            if (hasParen) {
                if (!check(")")) {
                    throw PreprocessorException("Expected ')': but '${peak<AnyToken>()}'")
                }
                kill()
            }
        }
        return create(filename, tokens, ctx).preprocess()
    }

    fun evaluateCondition(): Long {
        val preprocessed = preprocess()

        val parser = CProgramParser.build(filename, preprocessed)
        val constexpr = parser.constant_expression() ?:
            throw PreprocessorException("Cannot parse expression: '${TokenPrinter.print(preprocessed)}'")

        val evaluationContext = ConditionEvaluationContext(ctx)
        val constExpr = ConstEvalExpression.eval(constexpr, TryConstEvalExpressionLong(evaluationContext)) ?:
            throw PreprocessorException("Cannot evaluate expression: '${TokenPrinter.print(preprocessed)}'")
        return constExpr
    }
}