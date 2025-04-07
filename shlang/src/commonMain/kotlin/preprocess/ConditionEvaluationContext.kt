package preprocess

import codegen.consteval.ConstEvalContext
import parser.nodes.VarNode
import tokenizer.tokens.CToken
import typedesc.TypeHolder
import types.CompletedType
import types.INT


class ConditionEvaluationContext(private val preprocessorContext: PreprocessorContext): ConstEvalContext<Long> {
    private val defaultTypes = TypeHolder.create(::handleUndefinedVar)

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

    override fun typeHolder(): TypeHolder = defaultTypes

    companion object {
        private fun handleUndefinedVar(varNode: VarNode): CompletedType {
            // Assume that the undefined variable is an "SIGNED" integer
            return INT
        }
    }
}