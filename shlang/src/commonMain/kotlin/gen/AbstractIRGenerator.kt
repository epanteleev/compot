package gen

import gen.consteval.*
import ir.global.*
import ir.module.builder.impl.ModuleBuilder
import ir.types.*
import ir.value.Constant
import ir.value.InitializerListValue
import ir.value.StringLiteralConstant
import ir.value.Value
import parser.nodes.Expression
import parser.nodes.InitializerList
import parser.nodes.SingleInitializer
import parser.nodes.StringNode
import types.*


abstract class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack<Value>,
                                   protected val nameGenerator: NameGenerator) {
    protected fun constEvalExpression(lValueType: NonTrivialType, expr: Expression): Constant? {
        val result = constEvalExpression0(expr)
        if (result != null) {
            return Constant.of(lValueType, result)
        }
        return when (expr) {
            is InitializerList   -> constEvalInitializers(lValueType, expr)
            is SingleInitializer -> constEvalExpression(lValueType, expr.expr)
            is StringNode        -> StringLiteralConstant(expr.data())
            else -> throw IRCodeGenError("Unsupported expression $expr")
        }
    }

    private fun constEvalExpression0(expr: Expression): Number? = when (expr.resolveType(typeHolder)) {
        INT, SHORT, CHAR, UINT, USHORT, UCHAR -> {
            val ctx = CommonConstEvalContext<Int>(typeHolder)
            ConstEvalExpression.eval(expr, ConstEvalExpressionInt(ctx))
        }
        LONG, ULONG -> {
            val ctx = CommonConstEvalContext<Long>(typeHolder)
            ConstEvalExpression.eval(expr, ConstEvalExpressionLong(ctx))
        }
        FLOAT -> {
            val ctx = CommonConstEvalContext<Float>(typeHolder)
            ConstEvalExpression.eval(expr, ConstEvalExpressionFloat(ctx))
        }
        DOUBLE -> {
            val ctx = CommonConstEvalContext<Double>(typeHolder)
            ConstEvalExpression.eval(expr, ConstEvalExpressionDouble(ctx))
        }
        else -> null
    }

    private fun constEvalInitializers(lValueType: NonTrivialType, expr: InitializerList): InitializerListValue {
        if (lValueType !is AggregateType) {
            throw IRCodeGenError("Unsupported type $lValueType")
        }
        val elements = expr.initializers.mapIndexed { index, initializer ->
            val elementLValueType = lValueType.field(index)
            constEvalExpression(elementLValueType, initializer) ?: throw IRCodeGenError("Unsupported type $elementLValueType")
        }

        return InitializerListValue(lValueType, elements)
    }

    protected fun createStringLiteralName(): String {
        return nameGenerator.createStringLiteralName()
    }

    protected fun createGlobalConstantName(): String {
        return nameGenerator.createGlobalConstantName()
    }
}