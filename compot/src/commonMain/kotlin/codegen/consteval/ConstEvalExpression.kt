package codegen.consteval

import types.*
import parser.nodes.*
import parser.nodes.visitors.ExpressionVisitor
import sema.SemanticAnalysis
import typedesc.TypeHolder


data class ConstEvalException(override val message: String): Exception(message)

sealed class ConstEvalExpression<T>: ExpressionVisitor<T?> {
    companion object {
        inline fun <reified T : Number?> eval(expression: Expression, ctx: ConstEvalExpression<T>): T? {
            return expression.accept(ctx)
        }

        fun eval(expr: Expression, sema: SemanticAnalysis): Number? = when (expr.accept(sema)) {
            BOOL, INT, SHORT, CHAR, UINT, USHORT, UCHAR, is CEnumType -> {
                val ctx = CommonConstEvalContext<Int>(sema)
                eval(expr, TryConstEvalExpressionInt(ctx))
            }
            LONG, ULONG -> {
                val ctx = CommonConstEvalContext<Long>(sema)
                eval(expr, TryConstEvalExpressionLong(ctx))
            }
            FLOAT -> {
                val ctx = CommonConstEvalContext<Float>(sema)
                eval(expr, TryConstEvalExpressionFloat(ctx))
            }
            DOUBLE -> {
                val ctx = CommonConstEvalContext<Double>(sema)
                eval(expr, TryConstEvalExpressionDouble(ctx))
            }
            else -> null
        }
    }
}

//TODO copy-pasted many times
class TryConstEvalExpressionInt(private val ctx: ConstEvalContext<Int>): ConstEvalExpression<Int?>() {
    override fun visit(expression: CompoundLiteral): Int {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaEnd): Int? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaArg): Int? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaStart): Int? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaCopy): Int? {
        TODO("Not yet implemented")
    }

    override fun visit(unaryOp: UnaryOp): Int? {
        val primary = unaryOp.primary.accept(this) ?: return null
        return when (unaryOp.opType) {
            PrefixUnaryOpType.NEG     -> -primary
            PrefixUnaryOpType.NOT     -> if (primary == 0) 1 else 0
            PrefixUnaryOpType.BIT_NOT -> primary.inv()
            PrefixUnaryOpType.INC     -> primary + 1
            PrefixUnaryOpType.DEC     -> primary - 1
            PostfixUnaryOpType.INC    -> primary + 1
            PostfixUnaryOpType.DEC    -> primary - 1
            PrefixUnaryOpType.DEREF   -> null
            PrefixUnaryOpType.ADDRESS -> null
            PrefixUnaryOpType.PLUS    -> null
        }
    }

    override fun visit(binop: BinaryOp): Int? {
        val left = binop.left.accept(this) ?: return null
        val right = binop.right.accept(this) ?: return null
        when (binop.opType) {
            BinaryOpType.AND -> return if (left != 0 && right != 0) 1 else 0
            BinaryOpType.OR  -> return if (left != 0 || right != 0) 1 else 0
            else -> {}
        }

        return when (binop.opType) {
            BinaryOpType.ADD      -> left + right
            BinaryOpType.SUB      -> left - right
            BinaryOpType.MUL      -> left * right
            BinaryOpType.DIV      -> left / right
            BinaryOpType.MOD      -> left % right
            BinaryOpType.LT       -> when (binop.accept(ctx.semanticAnalysis())) {
                is AnyCUnsigned -> if (left.toUInt() < right.toUInt()) 1 else 0
                else -> if (left < right) 1 else 0
            }
            BinaryOpType.GT       -> when (binop.accept(ctx.semanticAnalysis())) {
                is AnyCUnsigned -> if (left.toUInt() > right.toUInt()) 1 else 0
                else -> if (left > right) 1 else 0
            }
            BinaryOpType.LE       -> when (binop.accept(ctx.semanticAnalysis())) {
                is AnyCUnsigned -> if (left.toUInt() <= right.toUInt()) 1 else 0
                else -> if (left <= right) 1 else 0
            }
            BinaryOpType.GE       -> when (binop.accept(ctx.semanticAnalysis())) {
                is AnyCUnsigned -> if (left.toUInt() >= right.toUInt()) 1 else 0
                else -> if (left >= right) 1 else 0
            }
            BinaryOpType.EQ       -> if (left == right) 1 else 0
            BinaryOpType.NE       -> if (left != right) 1 else 0
            BinaryOpType.BIT_AND  -> left and right
            BinaryOpType.BIT_OR   -> left or right
            BinaryOpType.BIT_XOR  -> left xor right
            BinaryOpType.SHL      -> left shl right
            BinaryOpType.SHR      -> when (binop.accept(ctx.semanticAnalysis())) {
                is AnyCUnsigned -> left ushr right
                else -> left shr right
            }
            else -> throw ConstEvalException("Cannot evaluate binary operator ${binop.opType}")
        }
    }

    override fun visit(conditional: Conditional): Int? {
        val cond = conditional.cond.accept(this) ?: return null
        return if (cond != 0) {
            conditional.eTrue.accept(this)
        } else {
            conditional.eFalse.accept(this)
        }
    }

    override fun visit(functionCall: FunctionCall): Int? {
        val primary = functionCall.primary
        if (primary !is VarNode) {
            return null
        }
        val evaluated = functionCall.args.map {
            it.accept(this) ?: return null
        }

        return ctx.callFunction(primary.nameIdent(), evaluated)
    }

    override fun visit(arrayAccess: ArrayAccess): Int? {
        return null
    }

    override fun visit(stringNode: StringNode): Int {
        TODO("Not yet implemented")
    }

    override fun visit(assignment: CharNode): Int? {
        return assignment.toByte().toInt()
    }

    override fun visit(sizeOf: SizeOf): Int {
        return sizeOf.constEval(ctx.semanticAnalysis())
    }

    override fun visit(cast: Cast): Int? {
        val expression = eval(cast.cast, ctx.semanticAnalysis()) ?: return null
        val type = cast.typeName.accept(ctx.semanticAnalysis()).typeDesc
        return when (type.cType()) {
            is AnyCInteger -> expression.toInt()
            else -> null
        }
    }

    override fun visit(numNode: NumNode): Int {
        val number = numNode.number.number()
        return number.toInt()
    }

    override fun visit(varNode: VarNode): Int? {
        val ret = ctx.getVariable(varNode.nameIdent())
        return ret
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess): Int? {
        return null
    }

    override fun visit(memberAccess: MemberAccess): Int? {
        return null
    }

    override fun visit(emptyExpression: EmptyExpression): Int? {
        return null
    }
}

class TryConstEvalExpressionLong(private val ctx: ConstEvalContext<Long>): ConstEvalExpression<Long?>() {
    override fun visit(builtin: BuiltinVaEnd): Long? {
        TODO("Not yet implemented")
    }

    override fun visit(expression: CompoundLiteral): Long {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaArg): Long? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaStart): Long? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaCopy): Long? {
        TODO("Not yet implemented")
    }

    override fun visit(unaryOp: UnaryOp): Long? {
        val primary = unaryOp.primary.accept(this) ?: return null
        return when (unaryOp.opType) {
            PrefixUnaryOpType.NEG     -> -primary
            PrefixUnaryOpType.NOT     -> if (primary == 0L) 1L else 0L
            PrefixUnaryOpType.BIT_NOT -> primary.inv()
            PrefixUnaryOpType.INC     -> primary + 1
            PrefixUnaryOpType.DEC     -> primary - 1
            PostfixUnaryOpType.INC    -> primary + 1
            PostfixUnaryOpType.DEC    -> primary - 1
            PrefixUnaryOpType.DEREF   -> null
            PrefixUnaryOpType.ADDRESS -> null
            PrefixUnaryOpType.PLUS    -> null
        }
    }

    override fun visit(binop: BinaryOp): Long? {
        val left = binop.left.accept(this) ?: return null
        val right = binop.right.accept(this) ?: return null
        when (binop.opType) {
            BinaryOpType.AND -> return if (left != 0L && right != 0L) 1 else 0
            BinaryOpType.OR  -> return if (left != 0L || right != 0L) 1 else 0
            else -> {}
        }

        return when (binop.opType) {
            BinaryOpType.ADD -> left + right
            BinaryOpType.SUB -> left - right
            BinaryOpType.MUL -> left * right
            BinaryOpType.DIV -> left / right
            BinaryOpType.MOD -> left % right
            BinaryOpType.LT -> when (binop.accept(ctx.semanticAnalysis())) {
                is AnyCUnsigned -> if (left.toULong() < right.toULong()) 1 else 0
                else -> if (left < right) 1 else 0
            }
            BinaryOpType.GT -> when (binop.accept(ctx.semanticAnalysis())) {
                is AnyCUnsigned -> if (left.toULong() > right.toULong()) 1 else 0
                else -> if (left > right) 1 else 0
            }
            BinaryOpType.LE -> when (binop.accept(ctx.semanticAnalysis())) {
                is AnyCUnsigned -> if (left.toULong() <= right.toULong()) 1 else 0
                else -> if (left <= right) 1 else 0
            }
            BinaryOpType.GE -> when (binop.accept(ctx.semanticAnalysis())) {
                is AnyCUnsigned -> if (left.toULong() >= right.toULong()) 1 else 0
                else -> if (left >= right) 1 else 0
            }
            BinaryOpType.EQ -> if (left == right) 1 else 0
            BinaryOpType.NE -> if (left != right) 1 else 0
            BinaryOpType.BIT_AND -> left and right
            BinaryOpType.BIT_OR -> left or right
            BinaryOpType.BIT_XOR -> left xor right
            BinaryOpType.SHL -> left shl right.toInt()
            BinaryOpType.SHR -> when (binop.accept(ctx.semanticAnalysis())) {
                is AnyCUnsigned -> left ushr right.toInt()
                else -> left shr right.toInt()
            }
            else -> throw ConstEvalException("Cannot evaluate binary operator ${binop.opType}")
        }
    }

    override fun visit(conditional: Conditional): Long? {
        val cond = conditional.cond.accept(this) ?: return null
        return if (cond != 0L) {
            conditional.eTrue.accept(this)
        } else {
            conditional.eFalse.accept(this)
        }
    }

    override fun visit(functionCall: FunctionCall): Long? {
        val primary = functionCall.primary
        if (primary !is VarNode) {
            throw ConstEvalException("Cannot evaluate function call with primary $primary")
        }

        val evaluated = functionCall.args.map { it.accept(this) ?: return null }
        return ctx.callFunction(primary.nameIdent(), evaluated)
    }

    override fun visit(arrayAccess: ArrayAccess): Long? = null

    override fun visit(stringNode: StringNode): Long {
        TODO("Not yet implemented")
    }

    override fun visit(assignment: CharNode): Long {
        return assignment.toByte().toLong()
    }

    override fun visit(sizeOf: SizeOf): Long {
        return sizeOf.constEval(ctx.semanticAnalysis()).toLong()
    }

    override fun visit(cast: Cast): Long? {
        val expression = eval(cast.cast, ctx.semanticAnalysis()) ?: return null
        val type = cast.typeName.accept(ctx.semanticAnalysis()).typeDesc
        return when (type.cType()) {
            is AnyCInteger -> expression.toLong()
            else -> null
        }
    }

    override fun visit(numNode: NumNode): Long? {
        val num = numNode.number.number()
        return num.toLong()
    }

    override fun visit(varNode: VarNode): Long? {
        return ctx.getVariable(varNode.nameIdent())
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess): Long? {
        return null
    }

    override fun visit(memberAccess: MemberAccess): Long? = null

    override fun visit(emptyExpression: EmptyExpression): Long? = null
}

class TryConstEvalExpressionFloat(private val ctx: ConstEvalContext<Float>): ConstEvalExpression<Float>() {
    override fun visit(expression: CompoundLiteral): Float {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaEnd): Float? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaArg): Float? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaStart): Float? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaCopy): Float? {
        TODO("Not yet implemented")
    }

    override fun visit(unaryOp: UnaryOp): Float? {
        val primary = unaryOp.primary.accept(this) ?: return null
        return when (unaryOp.opType) {
            PrefixUnaryOpType.NEG -> -primary
            PrefixUnaryOpType.INC -> primary + 1
            PrefixUnaryOpType.DEC -> primary - 1
            PostfixUnaryOpType.INC -> primary + 1
            PostfixUnaryOpType.DEC -> primary - 1
            PrefixUnaryOpType.NOT -> TODO()
            PrefixUnaryOpType.DEREF -> TODO()
            PrefixUnaryOpType.ADDRESS -> TODO()
            PrefixUnaryOpType.PLUS -> primary
            PrefixUnaryOpType.BIT_NOT -> TODO()
        }
    }

    override fun visit(binop: BinaryOp): Float? {
        val left = binop.left.accept(this) ?: return null
        val right = binop.right.accept(this) ?: return null
        return when (binop.opType) {
            BinaryOpType.ADD -> left + right
            BinaryOpType.SUB -> left - right
            BinaryOpType.MUL -> left * right
            BinaryOpType.DIV -> left / right
            BinaryOpType.MOD -> left % right
            BinaryOpType.LT -> if (left < right) 1f else 0f
            BinaryOpType.GT -> if (left > right) 1f else 0f
            BinaryOpType.LE -> if (left <= right) 1f else 0f
            BinaryOpType.GE -> if (left >= right) 1f else 0f
            BinaryOpType.EQ -> if (left == right) 1f else 0f
            BinaryOpType.NE -> if (left != right) 1f else 0f
            BinaryOpType.AND -> if (left != 0f && right != 0f) 1f else 0f
            BinaryOpType.OR -> if (left != 0f || right != 0f) 1f else 0f
            else -> throw ConstEvalException("Cannot evaluate binary operator ${binop.opType}")
        }
    }

    override fun visit(conditional: Conditional): Float? {
        val result = conditional.cond.accept(this) ?: return null
        return if (compare(result, 0f) == 0) {
            conditional.eTrue.accept(this)
        } else {
            conditional.eFalse.accept(this)
        }
    }

    override fun visit(functionCall: FunctionCall): Float? {
        val primary = functionCall.primary
        if (primary !is VarNode) {
            throw ConstEvalException("Cannot evaluate function call with primary $primary")
        }
        val evaluated = functionCall.args.map { it.accept(this) ?: return null }
        return ctx.callFunction(primary.nameIdent(), evaluated)
    }

    override fun visit(arrayAccess: ArrayAccess): Float {
        TODO("Not yet implemented")
    }

    override fun visit(stringNode: StringNode): Float {
        TODO("Not yet implemented")
    }

    override fun visit(assignment: CharNode): Float {
        TODO("Not yet implemented")
    }

    override fun visit(sizeOf: SizeOf): Float {
        return sizeOf.constEval(ctx.semanticAnalysis()).toFloat()
    }

    override fun visit(cast: Cast): Float {
        TODO("Not yet implemented")
    }

    override fun visit(numNode: NumNode): Float {
        return numNode.number.number().toFloat()
    }

    override fun visit(varNode: VarNode): Float? {
        return ctx.getVariable(varNode.nameIdent())
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess): Float? = null

    override fun visit(memberAccess: MemberAccess): Float {
        TODO("Not yet implemented")
    }

    override fun visit(emptyExpression: EmptyExpression): Float {
        TODO("Not yet implemented")
    }

    companion object {
        private fun compare(d1: Float, d2: Float): Int {
            if (d1 < d2) {
                return -1
            } else if (d1 > d2) {
                return 1
            } else {
                val thisBits = d1.toRawBits()
                val anotherBits =  d2.toRawBits()
                return if (thisBits == anotherBits) 0 else (if (thisBits < anotherBits) -1 else 1)
            }
        }
    }
}

class TryConstEvalExpressionDouble(private val ctx: ConstEvalContext<Double>): ConstEvalExpression<Double?>() {
    override fun visit(expression: CompoundLiteral): Double {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaArg): Double? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaStart): Double? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaEnd): Double? {
        TODO("Not yet implemented")
    }

    override fun visit(builtin: BuiltinVaCopy): Double? {
        TODO("Not yet implemented")
    }

    override fun visit(unaryOp: UnaryOp): Double? {
        val primary = unaryOp.primary.accept(this) ?: return null
        return when (unaryOp.opType) {
            PrefixUnaryOpType.NEG -> -primary
            PrefixUnaryOpType.INC -> primary + 1
            PrefixUnaryOpType.DEC -> primary - 1
            PostfixUnaryOpType.INC -> primary + 1
            PostfixUnaryOpType.DEC -> primary - 1
            PrefixUnaryOpType.NOT -> TODO()
            PrefixUnaryOpType.DEREF -> TODO()
            PrefixUnaryOpType.ADDRESS -> TODO()
            PrefixUnaryOpType.PLUS -> primary
            PrefixUnaryOpType.BIT_NOT -> TODO()
        }
    }

    override fun visit(binop: BinaryOp): Double? {
        val left = binop.left.accept(this) ?: return null
        val right = binop.right.accept(this) ?: return null
        return when (binop.opType) {
            BinaryOpType.ADD -> left + right
            BinaryOpType.SUB -> left - right
            BinaryOpType.MUL -> left * right
            BinaryOpType.DIV -> left / right
            BinaryOpType.MOD -> left % right
            BinaryOpType.LT -> if (left < right) 1.0 else 0.0
            BinaryOpType.GT -> if (left > right) 1.0 else 0.0
            BinaryOpType.LE -> if (left <= right) 1.0 else 0.0
            BinaryOpType.GE -> if (left >= right) 1.0 else 0.0
            BinaryOpType.EQ -> if (left == right) 1.0 else 0.0
            BinaryOpType.NE -> if (left != right) 1.0 else 0.0
            BinaryOpType.AND -> if (left != 0.0 && right != 0.0) 1.0 else 0.0
            BinaryOpType.OR -> if (left != 0.0 || right != 0.0) 1.0 else 0.0
            else -> throw ConstEvalException("Cannot evaluate binary operator ${binop.opType}")
        }
    }


    override fun visit(conditional: Conditional): Double? {
        val result = conditional.cond.accept(this) ?: return null
        return if (compare(result, 0.0) == 0) {
            conditional.eTrue.accept(this)
        } else {
            conditional.eFalse.accept(this)
        }
    }

    override fun visit(functionCall: FunctionCall): Double? {
        val primary = functionCall.primary
        if (primary !is VarNode) {
            throw ConstEvalException("Cannot evaluate function call with primary $primary")
        }
        val evaluated = functionCall.args.map { it.accept(this) ?: return null }
        return ctx.callFunction(primary.nameIdent(), evaluated)
    }

    override fun visit(arrayAccess: ArrayAccess): Double? = null

    override fun visit(stringNode: StringNode): Double {
        TODO("Not yet implemented")
    }

    override fun visit(assignment: CharNode): Double {
        TODO("Not yet implemented")
    }

    override fun visit(sizeOf: SizeOf): Double {
        return sizeOf.constEval(ctx.semanticAnalysis()).toDouble()
    }

    override fun visit(cast: Cast): Double? {
        val expression = eval(cast.cast, ctx.semanticAnalysis()) ?: return null
        val type = cast.typeName.accept(ctx.semanticAnalysis()).typeDesc
        return when (type.cType()) {
            is AnyCFloat -> expression.toDouble()
            else -> null
        }
    }

    override fun visit(numNode: NumNode): Double {
        return numNode.number.number().toDouble()
    }

    override fun visit(varNode: VarNode): Double? {
        return ctx.getVariable(varNode.nameIdent())
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess): Double {
        TODO("Not yet implemented")
    }

    override fun visit(memberAccess: MemberAccess): Double {
        TODO("Not yet implemented")
    }

    override fun visit(emptyExpression: EmptyExpression): Double {
        TODO("Not yet implemented")
    }

    companion object {
        private fun compare(d1: Double, d2: Double): Int {
            if (d1 < d2) {
                return -1
            } else if (d1 > d2) {
                return 1
            } else {
                val thisBits = d1.toRawBits()
                val anotherBits =  d2.toRawBits()
                return if (thisBits == anotherBits) 0 else (if (thisBits < anotherBits) -1 else 1)
            }
        }
    }
}