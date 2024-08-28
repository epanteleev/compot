package gen

import gen.consteval.*
import ir.global.ArrayGlobalConstant
import ir.global.GlobalConstant
import ir.global.StringLiteralConstant
import ir.global.StructGlobalConstant
import ir.module.builder.impl.ModuleBuilder
import ir.types.ArrayType
import ir.types.NonTrivialType
import ir.types.StructType
import ir.types.Type
import ir.value.Constant
import parser.nodes.Expression
import parser.nodes.InitializerList
import parser.nodes.StringNode
import types.*


abstract class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack,
                                   protected val nameGenerator: NameGenerator) {
    protected fun constEvalExpression(lValueType: NonTrivialType, expr: Expression): GlobalConstant? {
        val result = constEvalExpression0(expr)
        if (result != null) {
            return GlobalConstant.of(createGlobalConstantName(), lValueType, result)
        }
        return aggregateInitializer(lValueType, expr)
    }

    private fun constEvalExpression0(expr: Expression): Number? {
        val type = expr.resolveType(typeHolder)

        return when (type) {
            CType.INT, CType.SHORT, CType.CHAR, CType.UINT, CType.USHORT, CType.UCHAR -> {
                val ctx = CommonConstEvalContext<Int>(typeHolder)
                ConstEvalExpression.eval(expr, ConstEvalExpressionInt(ctx))
            }
            CType.LONG, CType.ULONG -> {
                val ctx = CommonConstEvalContext<Long>(typeHolder)
                ConstEvalExpression.eval(expr, ConstEvalExpressionLong(ctx))
            }
            CType.FLOAT -> {
                val ctx = CommonConstEvalContext<Float>(typeHolder)
                ConstEvalExpression.eval(expr, ConstEvalExpressionFloat(ctx))
            }
            CType.DOUBLE -> {
                val ctx = CommonConstEvalContext<Double>(typeHolder)
                ConstEvalExpression.eval(expr, ConstEvalExpressionDouble(ctx))
            }
            else -> null
        }
    }

    protected fun createStringLiteralName(): String {
        return nameGenerator.createStringLiteralName()
    }

    protected fun createGlobalConstantName(): String {
        return nameGenerator.createGlobalConstantName()
    }

    private fun aggregateInitializer(lValueType: NonTrivialType, expr: Expression): GlobalConstant? = when (expr) {
        is StringNode -> {
            val content = expr.data()
            StringLiteralConstant(createStringLiteralName(), ArrayType(Type.U8, content.length), content)
        }
        is InitializerList -> when (lValueType) {
            is ArrayType -> {
                val elements = expr.initializers.map { constEvalExpression0(it) }
                val convertedElements = elements.mapIndexed { it, num ->
                    Constant.of(lValueType.field(it), num as Number)
                }

                ArrayGlobalConstant(createStringLiteralName(), lValueType, convertedElements)
            }

            is StructType -> {
                val elements = expr.initializers.map { constEvalExpression0(it) }
                val convertedElements = elements.mapIndexed { it, num ->
                    Constant.of(lValueType.field(it), num as Number)
                }

                StructGlobalConstant(createStringLiteralName(), lValueType, convertedElements)
            }

            else -> throw IRCodeGenError("Unsupported type $lValueType")
        }
        else -> null
    }
}