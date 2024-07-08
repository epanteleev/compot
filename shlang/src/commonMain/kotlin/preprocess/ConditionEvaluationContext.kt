package preprocess

import gen.consteval.ConstEvalContext
import tokenizer.CToken
import types.TypeHolder


class ConditionEvaluationContext(private val preprocessorContext: PreprocessorContext): ConstEvalContext<Long> {
    override fun getVariable(name: CToken): Long {
        val predefined = preprocessorContext.findPredefinedMacros(name.str())
        if (predefined != null) {
            return predefined.constEval()
        }

        val has = preprocessorContext.hasMacroDefinition(name.str())
        return if (has) 1 else 0
    }

    override fun callFunction(name: CToken, args: List<Long>): Long {
        throw PreprocessorException("Unknown function '${name.str()}' in \"${name.position().filename()}\" at ${name.line()}:${name.pos()}")
    }

    override fun typeHolder(): TypeHolder {
        TODO("Not yet implemented")
    }
}