package parser.nodes

import codegen.consteval.*
import typedesc.TypeHolder
import tokenizer.tokens.Identifier
import parser.nodes.visitors.UnclassifiedNodeVisitor


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
        val constEval = ConstEvalExpression.eval(constExpression, TryConstEvalExpressionLong(ctx))
        if (constEval == null) {
            throw Exception("Cannot evaluate array designator")
        }

        return constEval
    }
}

class MemberDesignator(val name: Identifier): Designator() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun name(): String = name.str()
}