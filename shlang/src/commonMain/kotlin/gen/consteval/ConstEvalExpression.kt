package gen.consteval

import types.*
import parser.nodes.*
import parser.nodes.visitors.ExpressionVisitor


data class ConstEvalException(override val message: String): Exception(message)

abstract class ConstEvalExpression<T>: ExpressionVisitor<T?> {
    companion object {
        inline fun <reified T : Number?> eval(expression: Expression, ctx: ConstEvalExpression<T>): T? {
            return expression.accept(ctx)
        }
    }
}

//TODO copy-pasted many times
class TryConstEvalExpressionInt(private val ctx: ConstEvalContext<Int>): ConstEvalExpression<Int?>() {
    override fun visit(identNode: IdentNode): Int {
        throw ConstEvalException("identifier=${identNode}")
    }

    override fun visit(expression: CompoundLiteral): Int {
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
        when (binop.opType) {
            BinaryOpType.AND -> return if (binop.left.accept(this) != 0 && binop.right.accept(this) != 0) 1 else 0
            BinaryOpType.OR  -> return if (binop.left.accept(this) != 0 || binop.right.accept(this) != 0) 1 else 0
            else -> {}
        }
        val left = binop.left.accept(this) ?: return null
        val right = binop.right.accept(this) ?: return null
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
            BinaryOpType.BIT_AND  -> left and right
            BinaryOpType.BIT_OR   -> left or right
            BinaryOpType.BIT_XOR  -> left xor right
            BinaryOpType.SHL   -> left shl right
            BinaryOpType.SHR   -> left shr right
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
        val evaluated = functionCall.args.map {
            it.accept(this) ?: return null
        }

        return ctx.callFunction(functionCall.nameIdentifier(), evaluated)
    }

    override fun visit(arrayAccess: ArrayAccess): Int? {
        return null
    }

    override fun visit(stringNode: StringNode): Int {
        TODO("Not yet implemented")
    }

    override fun visit(assignment: CharNode): Int? {
        return assignment.toInt()
    }

    override fun visit(initializerList: InitializerList): Int? {
        if (initializerList.initializers.size != 1) {
            return null
        }
        return initializerList.initializers[0].accept(this)
    }

    override fun visit(singleInitializer: SingleInitializer): Int? {
        return singleInitializer.expr.accept(this)
    }

    override fun visit(designationInitializer: DesignationInitializer): Int {
        TODO("Not yet implemented")
    }

    override fun visit(sizeOf: SizeOf): Int {
        return sizeOf.constEval(ctx.typeHolder())
    }

    override fun visit(cast: Cast): Int? {
        val expression = cast.cast.accept(this) ?: return null
        val type = cast.typeName.specifyType(ctx.typeHolder(), listOf()).type
        return when (type.baseType()) {
            INT -> expression.toInt()
            LONG, ULONG -> expression.toInt()
            SHORT -> expression.toInt()
            else -> null
        }
    }

    override fun visit(numNode: NumNode): Int? {
        val number = numNode.number.toNumberOrNull() ?: throw ConstEvalException("Cannot evaluate number ${numNode.number}")
        if (number !is Number) {
            return null
        }
        return number.toInt()
    }

    override fun visit(varNode: VarNode): Int? {
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

    override fun visit(funcPointerCall: FuncPointerCall): Int {
        TODO("Not yet implemented")
    }
}

class TryConstEvalExpressionLong(private val ctx: ConstEvalContext<Long>): ConstEvalExpression<Long?>() {
    override fun visit(identNode: IdentNode): Long {
        throw ConstEvalException("identifier=${identNode}")
    }

    override fun visit(expression: CompoundLiteral): Long {
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
        when (binop.opType) {
            BinaryOpType.AND -> return if (binop.left.accept(this) != 0L && binop.right.accept(this) != 0L) 1 else 0
            BinaryOpType.OR  -> return if (binop.left.accept(this) != 0L || binop.right.accept(this) != 0L) 1 else 0
            else -> {}
        }
        val left = binop.left.accept(this) ?: return null
        val right = binop.right.accept(this) ?: return null
        return when (binop.opType) {
            BinaryOpType.ADD -> left + right
            BinaryOpType.SUB -> left - right
            BinaryOpType.MUL -> left * right
            BinaryOpType.DIV -> left / right
            BinaryOpType.MOD -> left % right
            BinaryOpType.LT -> if (left < right) 1 else 0
            BinaryOpType.GT -> if (left > right) 1 else 0
            BinaryOpType.LE -> if (left <= right) 1 else 0
            BinaryOpType.GE -> if (left >= right) 1 else 0
            BinaryOpType.EQ -> if (left == right) 1 else 0
            BinaryOpType.NE -> if (left != right) 1 else 0
            BinaryOpType.BIT_AND -> left and right
            BinaryOpType.BIT_OR -> left or right
            BinaryOpType.BIT_XOR -> left xor right
            BinaryOpType.SHL -> left shl right.toInt()
            BinaryOpType.SHR -> left shr right.toInt()
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
        val evaluated = functionCall.args.map { it.accept(this)?: return null }
        return ctx.callFunction(functionCall.nameIdentifier(), evaluated)
    }

    override fun visit(arrayAccess: ArrayAccess): Long {
        TODO("Not yet implemented")
    }

    override fun visit(stringNode: StringNode): Long {
        TODO("Not yet implemented")
    }

    override fun visit(assignment: CharNode): Long {
        TODO("Not yet implemented")
    }

    override fun visit(initializerList: InitializerList): Long {
        TODO("Not yet implemented")
    }

    override fun visit(singleInitializer: SingleInitializer): Long? {
        return singleInitializer.expr.accept(this)
    }

    override fun visit(designationInitializer: DesignationInitializer): Long {
        TODO("Not yet implemented")
    }

    override fun visit(sizeOf: SizeOf): Long {
        return sizeOf.constEval(ctx.typeHolder()).toLong()
    }

    override fun visit(cast: Cast): Long? {
        val expression = cast.cast.accept(this) ?: return null
        val type = cast.typeName.specifyType(ctx.typeHolder(), listOf()).type
        val converted = when (type.baseType()) {
            INT -> expression.toInt()
            LONG, ULONG -> expression
            SHORT -> expression.toShort()
            else -> throw ConstEvalException("Cannot cast to type $type")
        }

        return converted.toLong()
    }

    override fun visit(numNode: NumNode): Long? {
        val num = numNode.number.toNumberOrNull() ?: return null
        if (num !is Number) {
            println("warning num=${num}")
            return (num as ULong).toLong()
        }

        return num.toLong()
    }

    override fun visit(varNode: VarNode): Long? {
        val variable = ctx.getVariable(varNode.nameIdent()) ?: return null
        return variable.toLong()
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess): Long {
        TODO("Not yet implemented")
    }

    override fun visit(memberAccess: MemberAccess): Long {
        TODO("Not yet implemented")
    }

    override fun visit(emptyExpression: EmptyExpression): Long {
        TODO("Not yet implemented")
    }

    override fun visit(funcPointerCall: FuncPointerCall): Long {
        TODO("Not yet implemented")
    }
}

class TryConstEvalExpressionFloat(private val ctx: ConstEvalContext<Float>): ConstEvalExpression<Float>() {
    override fun visit(identNode: IdentNode): Float {
        throw ConstEvalException("identifier=${identNode}")
    }

    override fun visit(expression: CompoundLiteral): Float {
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
        val evaluated = functionCall.args.map { it.accept(this) ?: return null }
        return ctx.callFunction(functionCall.nameIdentifier(), evaluated)
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

    override fun visit(initializerList: InitializerList): Float {
        TODO("Not yet implemented")
    }

    override fun visit(sizeOf: SizeOf): Float {
        return sizeOf.constEval(ctx.typeHolder()).toFloat()
    }

    override fun visit(cast: Cast): Float {
        TODO("Not yet implemented")
    }

    override fun visit(numNode: NumNode): Float {
        return numNode.number.toNumberOrNull() as Float
    }

    override fun visit(varNode: VarNode): Float? {
        val variable = ctx.getVariable(varNode.nameIdent()) ?: return null
        return variable.toFloat()
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess): Float {
        TODO("Not yet implemented")
    }

    override fun visit(designationInitializer: DesignationInitializer): Float {
        TODO("Not yet implemented")
    }

    override fun visit(singleInitializer: SingleInitializer): Float? {
        return singleInitializer.expr.accept(this)
    }

    override fun visit(memberAccess: MemberAccess): Float {
        TODO("Not yet implemented")
    }

    override fun visit(emptyExpression: EmptyExpression): Float {
        TODO("Not yet implemented")
    }

    override fun visit(funcPointerCall: FuncPointerCall): Float {
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
    override fun visit(identNode: IdentNode): Double {
        throw ConstEvalException("identifier=${identNode}")
    }

    override fun visit(expression: CompoundLiteral): Double {
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
        val evaluated = functionCall.args.map { it.accept(this) ?: return null }
        return ctx.callFunction(functionCall.nameIdentifier(), evaluated)
    }

    override fun visit(arrayAccess: ArrayAccess): Double {
        TODO("Not yet implemented")
    }

    override fun visit(stringNode: StringNode): Double {
        TODO("Not yet implemented")
    }

    override fun visit(assignment: CharNode): Double {
        TODO("Not yet implemented")
    }

    override fun visit(initializerList: InitializerList): Double {
        TODO("Not yet implemented")
    }

    override fun visit(sizeOf: SizeOf): Double {
        return sizeOf.constEval(ctx.typeHolder()).toDouble()
    }

    override fun visit(cast: Cast): Double {
        TODO("Not yet implemented")
    }

    override fun visit(numNode: NumNode): Double {
        return numNode.number.toNumberOrNull() as Double
    }

    override fun visit(varNode: VarNode): Double? {
        val variable = ctx.getVariable(varNode.nameIdent()) ?: return null
        return variable.toDouble()
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

    override fun visit(designationInitializer: DesignationInitializer): Double {
        TODO("Not yet implemented")
    }

    override fun visit(singleInitializer: SingleInitializer): Double? {
        return singleInitializer.expr.accept(this)
    }

    override fun visit(funcPointerCall: FuncPointerCall): Double {
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