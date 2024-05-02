package preprocess

import gen.consteval.ConstEvalContext


class ConditionEvaluationContext(private val preprocessorContext: PreprocessorContext): ConstEvalContext {
    override fun getVariable(name: String): Int {
        val has = preprocessorContext.hasMacroDefinition(name)
        return if (has) 1 else 0
    }

    override fun callFunction(name: String, args: List<Int>): Int {
        when (name) {
            "defined" -> {
                if (args.size != 1) {
                    throw PreprocessorException("'defined' must have exactly one argument: '$args'")
                }

                return args[0]
            }
            else -> throw PreprocessorException("Unknown function $name")
        }
    }
}