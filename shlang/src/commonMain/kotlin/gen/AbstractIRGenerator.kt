package gen

import gen.TypeConverter.toIRType
import gen.consteval.*
import ir.attributes.GlobalValueAttribute
import ir.global.*
import ir.module.builder.impl.ModuleBuilder
import ir.types.*
import ir.value.Constant
import ir.value.InitializerListValue
import ir.value.PointerLiteral
import ir.value.PrimitiveConstant
import ir.value.StringLiteralConstant
import ir.value.Value
import parser.nodes.Expression
import parser.nodes.InitDeclarator
import parser.nodes.InitializerList
import parser.nodes.SingleInitializer
import parser.nodes.StringNode
import types.*


abstract class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack<Value>,
                                   protected val nameGenerator: NameGenerator) {
    protected fun constEvalExpression(lValueType: NonTrivialType, expr: Expression): Constant? {
        return when (expr) {
            is InitializerList   -> constEvalInitializers(lValueType, expr)
            is SingleInitializer -> constEvalExpression(lValueType, expr.expr)
            is StringNode        -> {
                val constant = expr.data()
                val stringLiteral = StringLiteralGlobalConstant(createGlobalConstantName(), ArrayType(Type.U8, constant.length), constant.toString())
                val g = mb.addConstant(stringLiteral)
                PointerLiteral(g)
            }
            else -> {
                val result = constEvalExpression0(expr)
                if (result != null) {
                    return Constant.of(lValueType, result)
                }
                throw IRCodeGenError("Unsupported expression $expr")
            }
        }
    }

    protected fun generateAssignmentDeclarator(decl: InitDeclarator): AnyGlobalValue {
        val cType = decl.cType()
        val lValueType = mb.toIRType<NonTrivialType>(typeHolder, cType.type.baseType())

        if (cType.storageClass == StorageClass.EXTERN) {
            val extern = mb.addExternValue(decl.name(), lValueType)
            varStack[decl.name()] = extern
            return extern
        }

        val attr = if (cType.storageClass == StorageClass.STATIC) {
            GlobalValueAttribute.INTERNAL
        } else {
            GlobalValueAttribute.DEFAULT
        }

        val constant = constEvalExpression(lValueType, decl.rvalue) ?: throw IRCodeGenError("Unsupported declarator '$decl'")
        when (constant) {
            is PointerLiteral -> when (lValueType) {
                is ArrayType -> {
                    val global = mb.addGlobal(decl.name(), lValueType, constant.gConstant.constant(), attr)
                    varStack[decl.name()] = global
                    return global
                }
                is PointerType -> {
                    val global = mb.addGlobal(decl.name(), lValueType, constant, attr)
                    varStack[decl.name()] = global
                    return global
                }
                else -> throw IRCodeGenError("Unsupported type $lValueType")
            }
            is PrimitiveConstant -> {
                val g = mb.addGlobal(decl.name(), lValueType, constant, attr)
                varStack[decl.name()] = g
                return g
            }
            is InitializerListValue -> {
                when (lValueType) {
                    is ArrayType -> {
                        val global = mb.addGlobal(decl.name(), lValueType, constant, attr)
                        varStack[decl.name()] = global
                        return global
                    }
                    is StructType -> {
                        val global = mb.addGlobal(decl.name(), lValueType, constant, attr)
                        varStack[decl.name()] = global
                        return global
                    }
                    else -> throw IRCodeGenError("Unsupported type $lValueType")
                }
            }
            else -> TODO("$constant")
        }
        throw IRCodeGenError("Unsupported declarator '$decl'")
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

    private fun constEvalInitializers(lValueType: NonTrivialType, expr: InitializerList): Constant {
        if (lValueType !is AggregateType) {
            throw IRCodeGenError("Unsupported type $lValueType")
        }

        if (expr.initializers.size == 1) {
            return constEvalExpression(lValueType, expr.initializers[0]) ?: throw IRCodeGenError("Unsupported type $lValueType")
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