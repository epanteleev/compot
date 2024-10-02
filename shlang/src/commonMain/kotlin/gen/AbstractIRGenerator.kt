package gen

import gen.TypeConverter.toIRLVType
import gen.TypeConverter.toIRType
import gen.consteval.*
import ir.Definitions.QWORD_SIZE
import ir.attributes.GlobalValueAttribute
import ir.global.*
import ir.module.ExternFunction
import ir.module.builder.impl.ModuleBuilder
import ir.types.*
import ir.value.Constant
import ir.value.InitializerListValue
import ir.value.PointerLiteral
import ir.value.PrimitiveConstant
import ir.value.StringLiteralConstant
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
import typedesc.StorageClass
import typedesc.TypeDesc
import typedesc.TypeHolder
import typedesc.VarDescriptor
import types.*


sealed class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack<Value>,
                                   protected val nameGenerator: NameGenerator) {
    protected fun constEvalExpression(lValueType: NonTrivialType, expr: Expression): Constant? = when (expr) {
        is InitializerList -> when (lValueType) {
            is IntegerType -> {
                if (expr.initializers.size != 1) {
                    throw IRCodeGenError("Unsupported initializer list size ${expr.initializers.size}")
                }
                constEvalExpression(lValueType, expr.initializers[0])
            }
            is ArrayType  -> constEvalInitializers(lValueType, expr)
            is StructType -> constEvalInitializers(lValueType, expr)
            else -> throw IRCodeGenError("Unsupported type $lValueType")
        }
        is SingleInitializer -> constEvalExpression(lValueType, expr.expr)
        is StringNode        -> when (lValueType) {
            is ArrayType -> StringLiteralConstant(expr.data())
            else -> {
                val constant = expr.data()
                val stringLiteral = StringLiteralGlobalConstant(createStringLiteralName(), ArrayType(Type.U8, constant.length), constant.toString())
                val g = mb.addConstant(stringLiteral)
                PointerLiteral(g)
            }
        }
        else -> defaultContEval(lValueType, expr)
    }

    private fun defaultContEval(lValueType: NonTrivialType, expr: Expression): Constant? {
        val result = constEvalExpression0(expr)
        return if (result != null) {
            Constant.of(lValueType, result)
        } else {
            null
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
            val lValueType = mb.toIRType<AggregateType>(typeHolder, varDesc.type.cType())
            val constant = constEvalExpression(lValueType, expr.initializerList) as InitializerListValue
            val gConstant = when (varDesc.type.cType()) {
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
        val lValueType = mb.toIRType<NonTrivialType>(typeHolder, cType.type.cType())

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
            is StringLiteralConstant -> {
                val global = mb.addGlobal(declarator.name(), lValueType, constant, attr)
                varStack[declarator.name()] = global
                return global
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
        val irType = mb.toIRType<NonTrivialType>(typeHolder, type.type.cType())
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
        when (val type = varDesc.type.cType()) {
            is CFunctionType -> {
                val abstrType = type.functionType
                val argTypes  = argumentTypes(abstrType.argsTypes, abstrType.retType)
                val returnType = irReturnType(abstrType.retType)

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
                    val zero = Constant.of(mb.toIRType<NonTrivialType>(typeHolder, field.second.cType()), 0)
                    elements.add(zero)
                }
                val constant = InitializerListValue(irType, elements)
                val global = mb.addGlobal(name, irType, constant)
                varStack[name] = global
                return global
            }
            is CUncompletedArrayType -> {
                return Value.UNDEF //TODO
            }
            else -> throw IRCodeGenError("Function or struct expected, but was '$type'")
        }
        throw IRCodeGenError("Unsupported declarator $declarator")
    }

    protected fun constEvalExpression0(expr: Expression): Number? = when (expr.resolveType(typeHolder)) {
        INT, SHORT, CHAR, UINT, USHORT, UCHAR -> {
            val ctx = CommonConstEvalContext<Int>(typeHolder)
            ConstEvalExpression.eval(expr, TryConstEvalExpressionInt(ctx))
        }
        LONG, ULONG -> {
            val ctx = CommonConstEvalContext<Long>(typeHolder)
            ConstEvalExpression.eval(expr, TryConstEvalExpressionLong(ctx))
        }
        FLOAT -> {
            val ctx = CommonConstEvalContext<Float>(typeHolder)
            ConstEvalExpression.eval(expr, TryConstEvalExpressionFloat(ctx))
        }
        DOUBLE -> {
            val ctx = CommonConstEvalContext<Double>(typeHolder)
            ConstEvalExpression.eval(expr, TryConstEvalExpressionDouble(ctx))
        }
        else -> null
    }

    private fun constEvalInitializers(lValueType: ArrayType, expr: InitializerList): Constant {
        if (expr.initializers.size == 1) {
            return constEvalExpression(lValueType, expr.initializers[0]) ?: throw IRCodeGenError("Unsupported type $lValueType")
        }

        val elements = expr.initializers.mapIndexed { index, initializer ->
            val elementLValueType = lValueType.field(index)
            constEvalExpression(elementLValueType, initializer) ?: let {
                throw IRCodeGenError("Unsupported type $elementLValueType")
            }
        }

        return InitializerListValue(lValueType, elements)
    }

    private fun constEvalInitializers(lValueType: StructType, expr: InitializerList): Constant {
        val elements = expr.initializers.mapIndexed { index, initializer ->
            val elementLValueType = lValueType.field(index)
            constEvalExpression(elementLValueType, initializer) ?: let {
                throw IRCodeGenError("Unsupported type $elementLValueType")
            }
        }

        return InitializerListValue(lValueType, elements)
    }

    protected fun argumentTypes(ctypes: List<TypeDesc>, retTypeDesc: TypeDesc): List<PrimitiveType> {
        val types = arrayListOf<PrimitiveType>()
        if (retTypeDesc.cType() is CAggregateType && retTypeDesc.cType().size() > QWORD_SIZE * 2) {
            types.add(Type.Ptr)
        }
        for (type in ctypes) {
            when (type.cType()) {
                is CStructType -> {
                    val irType = mb.toIRType<StructType>(typeHolder, type.cType())
                    val parameters = CallConvention.coerceArgumentTypes(irType)
                    if (parameters != null) {
                        types.addAll(parameters)
                    } else {
                        types.add(Type.Ptr)
                    }
                }
                is CArrayType, is CUncompletedArrayType -> types.add(Type.Ptr)
                is AnyCPointer -> types.add(Type.Ptr)
                is BOOL        -> types.add(Type.U8)
                is CPrimitive  -> types.add(mb.toIRType<PrimitiveType>(typeHolder, type.cType()))
                else -> throw IRCodeGenError("Unknown type, type=$type")
            }
        }
        return types
    }

    protected fun irReturnType(retType: TypeDesc): Type = when (retType.cType()) {
        is VOID -> Type.Void
        is CPrimitive -> mb.toIRLVType<PrimitiveType>(typeHolder, retType.cType())
        is CStructType -> {
            val structType = mb.toIRType<StructType>(typeHolder, retType.cType())
            val list = CallConvention.coerceArgumentTypes(structType) ?: return Type.Void
            if (list.size == 1) {
                list[0]
            } else {
                TupleType(list.toTypedArray())
            }
        }
        else -> throw IRCodeGenError("Unknown return type, type=$retType")
    }

    protected fun createStringLiteralName(): String {
        return nameGenerator.createStringLiteralName()
    }

    protected fun createGlobalConstantName(): String {
        return nameGenerator.createGlobalConstantName()
    }
}