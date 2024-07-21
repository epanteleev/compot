package gen

import gen.TypeConverter.toIRType
import gen.consteval.*
import ir.global.ArrayGlobalConstant
import ir.global.GlobalConstant
import ir.global.StringLiteralConstant
import types.TypeHolder
import ir.module.builder.impl.ModuleBuilder
import ir.types.AggregateType
import ir.types.ArrayType
import ir.types.Type
import ir.value.Constant
import parser.nodes.Expression
import parser.nodes.InitializerList
import parser.nodes.StringNode
import types.CType
import types.CompoundType


abstract class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack,
                                   var constantCounter: Int) {
    protected fun constEvalExpression(lValueType: Type, expr: Expression): GlobalConstant? {
        val result = constEvalExpression0(expr)
        if (result != null) {
            return GlobalConstant.of(createGlobalConstantName(), lValueType, result)
        }
        return stringLiteralInitializer(expr)
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
        return ".str${constantCounter++}"
    }

    protected fun createGlobalConstantName(): String {
        return ".v${constantCounter++}"
    }

    private fun stringLiteralInitializer(expr: Expression): GlobalConstant? {
        if (expr is StringNode) {
            val content = expr.str.data()
            return StringLiteralConstant(createStringLiteralName(), ArrayType(Type.U8, content.length), content)
        } else if (expr is InitializerList) {
            val type = expr.resolveType(typeHolder)

            if (type is CompoundType) {
                val typeExpr = mb.toIRType<AggregateType>(typeHolder, type)

                val elements = expr.initializers.map { constEvalExpression0(it) }
                val convertedElements = elements.mapIndexed { it, num ->
                    Constant.of(typeExpr.field(it), num as Number)
                }

                return ArrayGlobalConstant(createStringLiteralName(), typeExpr as ArrayType, convertedElements)
            } else {
                throw IRCodeGenError("Unsupported type $type")
            }
        } else {
            return null
        }
    }
}