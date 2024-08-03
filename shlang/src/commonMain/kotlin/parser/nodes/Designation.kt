package parser.nodes

import gen.consteval.CommonConstEvalContext
import gen.consteval.ConstEvalExpression
import gen.consteval.ConstEvalExpressionLong
import tokenizer.Identifier
import parser.nodes.visitors.UnclassifiedNodeVisitor
import types.TypeHolder


class Designation(val designators: List<Designator>): UnclassifiedNode() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

sealed class Designator: UnclassifiedNode()

class ArrayDesignator(val constExpression: Expression): Designator() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun constEval(typeHolder: TypeHolder): Long {
        val ctx = CommonConstEvalContext<Long>(typeHolder)
        return ConstEvalExpression.eval(constExpression, ConstEvalExpressionLong(ctx))
    }
}

class MemberDesignator(val name: Identifier): Designator() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun name(): String = name.str()
}