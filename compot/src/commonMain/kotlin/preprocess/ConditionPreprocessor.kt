package preprocess

import types.INT
import tokenizer.*
import tokenizer.tokens.*
import parser.CProgramParser
import codegen.consteval.ConstEvalExpression
import codegen.consteval.TryConstEvalExpressionLong
import preprocess.CProgramPreprocessor.Companion.create


internal class ConditionPreprocessor(filename: String, condition: TokenList, private val ctx: PreprocessorContext): AbstractCPreprocessor(filename, condition) {
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
            eatSpaces()
            if (!check<CToken>()) {
                throw PreprocessorException("Expected identifier: but '${peak<AnyToken>()}'")
            }
            val hasParen = check("(")
            if (hasParen) {
                kill()
            }
            killSpaces()
            val name = peak<CToken>()
            killWithSpaces()

            val num = if (ctx.findMacros(name.str()) == null) 0 else 1
            add(PPNumber(num, INT, name.position()))

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
        return ConstEvalExpression.eval(constexpr, TryConstEvalExpressionLong(evaluationContext)) ?:
            throw PreprocessorException("Cannot evaluate expression: '${TokenPrinter.print(preprocessed)}'")
    }
}