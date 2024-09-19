package gen

import gen.TypeConverter.toIRType
import gen.consteval.*
import ir.attributes.GlobalValueAttribute
import ir.global.*
import ir.module.ExternFunction
import ir.module.builder.impl.ModuleBuilder
import ir.types.*
import ir.value.Constant
import ir.value.InitializerListValue
import ir.value.PointerLiteral
import ir.value.PrimitiveConstant
import ir.value.Value
import parser.nodes.CompoundLiteral
import parser.nodes.Declarator
import parser.nodes.Expression
import parser.nodes.InitDeclarator
import parser.nodes.InitializerList
import parser.nodes.PrefixUnaryOpType
import parser.nodes.SingleInitializer
import parser.nodes.StringNode
import parser.nodes.UnaryOp
import parser.nodes.UnaryOpType
import types.*


abstract class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack<Value>,
                                   protected val nameGenerator: NameGenerator) {
    protected fun constEvalExpression(lValueType: NonTrivialType, expr: Expression): Constant? = when (expr) {
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
                Constant.of(lValueType, result)
            } else {
                null
            }
        }
    }

    private fun staticInitializer(expr: Expression): Constant? = when (expr) {
        is UnaryOp -> {
            val operand = staticInitializer(expr.primary) ?: throw IRCodeGenError("Unsupported: $expr")
            when (expr.opType) {
                is PrefixUnaryOpType -> when (expr.opType) {
                    PrefixUnaryOpType.ADDRESS -> operand
                    else -> throw IRCodeGenError("Unsupported unary operator ${expr.opType}")
                }
                else -> throw IRCodeGenError("Unsupported unary operator ${expr.opType}")
            }
        }
        is CompoundLiteral -> {
            val varDesc = expr.typeName.specifyType(typeHolder, listOf())
            val lValueType = mb.toIRType<NonTrivialType>(typeHolder, varDesc.type.baseType())
            val constant = constEvalInitializers(lValueType, expr.initializerList) as InitializerListValue
            val gConstant = when (varDesc.type.baseType()) {
                is CArrayType -> {
                    ArrayGlobalConstant(createGlobalConstantName(), constant)
                }
                is CStructType -> {
                    StructGlobalConstant(createGlobalConstantName(), constant)
                }
                else -> throw IRCodeGenError("Unsupported type $lValueType")
            }
            PointerLiteral(mb.addConstant(gConstant))
        }
        else -> TODO()
    }

    private fun toIrAttribute(storageClass: StorageClass?): GlobalValueAttribute? = when (storageClass) {
        StorageClass.STATIC -> GlobalValueAttribute.INTERNAL
        StorageClass.EXTERN -> null
        else -> GlobalValueAttribute.DEFAULT
    }

    protected fun generateGlobalAssignmentDeclarator(declarator: InitDeclarator): AnyGlobalValue {
        val cType = declarator.cType()
        val lValueType = mb.toIRType<NonTrivialType>(typeHolder, cType.type.baseType())

        if (cType.storageClass == StorageClass.EXTERN) {
            val extern = mb.addExternValue(declarator.name(), lValueType)
            varStack[declarator.name()] = extern
            return extern
        }

        val attr = toIrAttribute(cType.storageClass)!!

        val constant = constEvalExpression(lValueType, declarator.rvalue) ?: staticInitializer(declarator.rvalue)
        when (constant) {
            is PointerLiteral -> when (lValueType) {
                is ArrayType -> {
                    val global = mb.addGlobal(declarator.name(), lValueType, constant.gConstant.constant(), attr)
                    varStack[declarator.name()] = global
                    return global
                }
                is PointerType -> {
                    val global = mb.addGlobal(declarator.name(), lValueType, constant, attr)
                    varStack[declarator.name()] = global
                    return global
                }
                else -> throw IRCodeGenError("Unsupported type $lValueType")
            }
            is PrimitiveConstant -> {
                val g = mb.addGlobal(declarator.name(), lValueType, constant, attr)
                varStack[declarator.name()] = g
                return g
            }
            is InitializerListValue -> {
                when (lValueType) {
                    is ArrayType -> {
                        val global = mb.addGlobal(declarator.name(), lValueType, constant, attr)
                        varStack[declarator.name()] = global
                        return global
                    }
                    is StructType -> {
                        val global = mb.addGlobal(declarator.name(), lValueType, constant, attr)
                        varStack[declarator.name()] = global
                        return global
                    }
                    else -> throw IRCodeGenError("Unsupported type $lValueType")
                }
            }
            else -> TODO("$constant")
        }
        throw IRCodeGenError("Unsupported declarator '$declarator'")
    }

    private fun getExternFunction(name: String, returnType: Type, arguments: List<NonTrivialType>, isVararg: Boolean = false): ExternFunction {
        val externFunction = mb.findExternFunctionOrNull(name)
        if (externFunction != null) {
            println("Warning: extern function $name already exists") //TODO implement warning mechanism
            return externFunction
        }

        return mb.createExternFunction(name, returnType, arguments, isVararg)
    }

    private fun makeGlobalValue(name: String, type: VarDescriptor): Value {
        val irType = mb.toIRType<NonTrivialType>(typeHolder, type.type.baseType())
        val value = if (type.storageClass == StorageClass.EXTERN) {
            mb.addExternValue(name, irType)
        } else {
            val constant = Constant.of(irType, 0)
            mb.addGlobal(name, irType, constant)
        }
        varStack[name] = value
        return value
    }

    protected fun generateGlobalDeclarator(declarator: Declarator): Value {
        val name = declarator.name()
        val varDesc = declarator.cType()
        when (val type = varDesc.type.baseType()) {
            is CBaseFunctionType -> {
                val abstrType = type.functionType
                val argTypes  = abstrType.argsTypes.map {
                    mb.toIRType<NonTrivialType>(typeHolder, it.baseType())
                }
                val returnType = mb.toIRType<Type>(typeHolder, abstrType.retType.baseType())

                val isVararg = type.functionType.variadic
                val externFunc = getExternFunction(name, returnType, argTypes, isVararg)
                varStack[name] = externFunc
                return externFunc
            }
            is CPrimitive -> {
                return makeGlobalValue(name, varDesc)
            }
            is CArrayType -> {
                val irType = mb.toIRType<ArrayType>(typeHolder, type)
                if (varDesc.storageClass == StorageClass.EXTERN) {
                    val externValue = mb.addExternValue(name, irType)
                    varStack[name] = externValue
                    return externValue
                }
                val attr = toIrAttribute(varDesc.storageClass)!!
                val zero = Constant.of(irType.elementType(), 0)
                val elements = generateSequence { zero }.take(irType.length).toList()
                val constant = InitializerListValue(irType, elements)
                val global = mb.addGlobal(name, irType, constant, attr)
                varStack[name] = global
                return global
            }
            is CStructType -> {
                val irType = mb.toIRType<StructType>(typeHolder, type)
                if (varDesc.storageClass == StorageClass.EXTERN) {
                    val externValue = mb.addExternValue(name, irType)
                    varStack[name] = externValue
                    return externValue
                }
                val elements = arrayListOf<Constant>()
                for (field in type.fields()) {
                    val zero = Constant.of(mb.toIRType<NonTrivialType>(typeHolder, field.second.baseType()), 0)
                    elements.add(zero)
                }
                val constant = InitializerListValue(irType, elements)
                val global = mb.addGlobal(name, irType, constant)
                varStack[name] = global
                return global
            }
            else -> throw IRCodeGenError("Function or struct expected, but was '$type'")
        }
        throw IRCodeGenError("Unsupported declarator $declarator")
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