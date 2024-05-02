package preprocess

import gen.consteval.ConstEvalContext
import tokenizer.CToken
import tokenizer.Numeric


class ConditionEvaluationContext(private val preprocessorContext: PreprocessorContext): ConstEvalContext {
    override fun getVariable(name: CToken): Int {
        val predefined = preprocessorContext.findPredefinedMacros(name.str())
        if (predefined != null) {
            return predefined.constEval()
        }

        val has = preprocessorContext.hasMacroDefinition(name.str())
        return if (has) 1 else 0
    }

    override fun callFunction(name: CToken, args: List<Int>): Int {
        when (name.str()) {
            "defined" -> {
                if (args.size != 1) {
                    throw PreprocessorException("'defined' must have exactly one argument: '$args'")
                }

                return args[0]
            }
            else -> throw PreprocessorException("Unknown function '${name.str()}'")
        }
    }
}