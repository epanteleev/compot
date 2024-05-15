package gen.consteval

import parser.nodes.*
import parser.nodes.visitors.ExpressionVisitor


data class ConstEvalException(override val message: String): Exception(message)

class ConstEvalExpression private constructor(private val ctx: ConstEvalContext): ExpressionVisitor<Int> {

    companion object {
        fun eval(expression: Expression, ctx: ConstEvalContext): Int {
            return expression.accept(ConstEvalExpression(ctx))
        }
    }

    override fun visit(identNode: IdentNode): Int {
        throw ConstEvalException("identifier=${identNode}")
    }

    override fun visit(unaryOp: UnaryOp): Int {
        return when (unaryOp.opType) {
            PrefixUnaryOpType.NEG     -> -unaryOp.primary.accept(this)
            PrefixUnaryOpType.NOT     -> if (unaryOp.primary.accept(this) == 0) 1 else 0
            PrefixUnaryOpType.BIT_NOT -> unaryOp.primary.accept(this).inv()
            PrefixUnaryOpType.INC     -> unaryOp.primary.accept(this) + 1
            PrefixUnaryOpType.DEC     -> unaryOp.primary.accept(this) - 1
            PostfixUnaryOpType.INC    -> unaryOp.primary.accept(this) + 1
            PostfixUnaryOpType.DEC    -> unaryOp.primary.accept(this) - 1
            else                      -> throw ConstEvalException("Cannot evaluate unary operator ${unaryOp.opType}")
        }
    }

    override fun visit(binop: BinaryOp): Int {
        val left = binop.left.accept(this)
        val right = binop.right.accept(this)
        return when (binop.opType) {
            BinaryOpType.ADD      -> left + right
            BinaryOpType.SUB      -> left - right
            BinaryOpType.MUL      -> left * right
            BinaryOpType.DIV      -> left / right
            BinaryOpType.MOD      -> left % right
            BinaryOpType.LT       -> if (left < right) 1 else 0
            BinaryOpType.GT       -> if (left > right) 1 else 0
            BinaryOpType.LE      -> if (left <= right) 1 else 0
            BinaryOpType.GE      -> if (left >= right) 1 else 0
            BinaryOpType.EQ       -> if (left == right) 1 else 0
            BinaryOpType.NE      -> if (left != right) 1 else 0
            BinaryOpType.AND      -> if (left != 0 && right != 0) 1 else 0
            BinaryOpType.OR       -> if (left != 0 || right != 0) 1 else 0
            BinaryOpType.BIT_AND  -> left and right
            BinaryOpType.BIT_OR   -> left or right
            BinaryOpType.BIT_XOR  -> left xor right
            BinaryOpType.SHL   -> left shl right
            BinaryOpType.SHR   -> left shr right
            else -> throw ConstEvalException("Cannot evaluate binary operator ${binop.opType}")
        }
    }

    override fun visit(conditional: Conditional): Int {
        return if (conditional.cond.accept(this) != 0) {
            conditional.eTrue.accept(this)
        } else {
            conditional.eFalse.accept(this)
        }
    }

    override fun visit(functionCall: FunctionCall): Int {
        val evaluated = functionCall.args.map { it.accept(this) }
        return ctx.callFunction(functionCall.nameIdentifier(), evaluated)
    }

    override fun visit(arrayAccess: ArrayAccess): Int {
        TODO("Not yet implemented")
    }

    override fun visit(stringNode: StringNode): Int {
        TODO("Not yet implemented")
    }

    override fun visit(initializerList: InitializerList): Int {
        TODO("Not yet implemented")
    }

    override fun visit(sizeOf: SizeOf): Int {
        TODO("Not yet implemented")
    }

    override fun visit(cast: Cast): Int {
        TODO("Not yet implemented")
    }

    override fun visit(numNode: NumNode): Int {
        return numNode.toLong.data.toInt()
    }

    override fun visit(varNode: VarNode): Int {
        return ctx.getVariable(varNode.nameIdent())
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess): Int {
        TODO("Not yet implemented")
    }

    override fun visit(memberAccess: MemberAccess): Int {
        TODO("Not yet implemented")
    }

    override fun visit(emptyExpression: EmptyExpression): Int {
        TODO("Not yet implemented")
    }
}