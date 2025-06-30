package parser.nodes

import codegen.consteval.*
import sema.SemanticAnalysis
import tokenizer.tokens.Identifier
import tokenizer.Position


class Designation(val designators: List<Designator>) {
    fun begin(): Position = designators.first().begin()
}

sealed class Designator {
    abstract fun begin(): Position
}

class ArrayDesignator(val constExpression: Expression): Designator() {
    override fun begin(): Position = constExpression.begin()

    fun constEval(sema: SemanticAnalysis): Long {
        val ctx = ArraySizeConstEvalContext(sema)
        return ConstEvalExpression.eval(constExpression, TryConstEvalExpressionLong(ctx))
            ?: throw Exception("Cannot evaluate array designator")
    }
}

class MemberDesignator(private val name: Identifier): Designator() {
    override fun begin(): Position = name.position()
    fun name(): String = name.str()
}