package gen

import types.*
import ir.types.*
import ir.value.*
import typedesc.*
import ir.global.*
import parser.nodes.*
import gen.consteval.*
import ir.module.ExternFunction
import ir.Definitions.QWORD_SIZE
import gen.TypeConverter.toIRType
import ir.attributes.GlobalValueAttribute
import ir.module.builder.impl.ModuleBuilder
import ir.value.constant.Constant
import ir.value.constant.InitializerListValue
import ir.value.constant.NonTrivialConstant
import ir.value.constant.PointerLiteral
import ir.value.constant.PrimitiveConstant
import ir.value.constant.StringLiteralConstant


sealed class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack<Value>,
                                   protected val nameGenerator: NameGenerator) {
    protected fun constEvalExpression(lValueType: NonTrivialType, expr: Expression): NonTrivialConstant? = when (expr) {
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
        is UnaryOp -> when (expr.opType) {
            is PrefixUnaryOpType -> when (expr.opType) {
                PrefixUnaryOpType.ADDRESS -> staticAddressOf(expr.primary) ?: defaultContEval(lValueType, expr)
                else -> defaultContEval(lValueType, expr)
            }
            else -> defaultContEval(lValueType, expr)
        }
        is SingleInitializer -> constEvalExpression(lValueType, expr.expr)
        is StringNode        -> when (lValueType) {
            is ArrayType -> StringLiteralConstant(ArrayType(Type.I8, expr.data().length) ,expr.data())
            else -> {
                val constant = expr.data()
                val stringLiteral = StringLiteralGlobalConstant(createStringLiteralName(), ArrayType(Type.U8, constant.length), constant.toString())
                PointerLiteral(mb.addConstant(stringLiteral))
            }
        }
        else -> defaultContEval(lValueType, expr)
    }

    private fun staticAddressOf(expr: Expression): NonTrivialConstant? {
        if (expr !is VarNode) {
            return null
        }
        val name = expr.name()
        val value = varStack[name] ?: return null
        value as GlobalValue
        return PointerLiteral(value)
    }

    private fun defaultContEval(lValueType: NonTrivialType, expr: Expression): NonTrivialConstant? {
        val result = constEvalExpression0(expr)
        return if (result != null) {
            NonTrivialConstant.of(lValueType, result)
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
                    val gc = constant.gConstant as GlobalConstant
                    val global = mb.addGlobal(declarator.name(), lValueType, gc.constant(), attr)
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
                val cArrayType = cType.type.cType() as CArrayType
                println("'${constant.data()}'")
                if (cArrayType.dimension > constant.data().length.toLong()) {
                    val stringBuilder = StringBuilder()
                    stringBuilder.append(constant.content)
                    for (i in 0 until cArrayType.dimension - constant.data().length) {
                        stringBuilder.append("\\0")
                    }
                    val content = stringBuilder.toString()
                    val newConstant = StringLiteralConstant(ArrayType(Type.I8, cArrayType.dimension.toInt()), content)
                    val global = mb.addGlobal(declarator.name(), lValueType, newConstant, attr)
                    varStack[declarator.name()] = global
                    return global
                } else {
                    val global = mb.addGlobal(declarator.name(), lValueType, constant, attr)
                    varStack[declarator.name()] = global
                    return global
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
                val zero = NonTrivialConstant.of(irType.elementType(), 0)
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
                val elements = arrayListOf<NonTrivialConstant>()
                for (field in type.fields()) {
                    val irType = mb.toIRType<NonTrivialType>(typeHolder, field.cType())
                    val zero = NonTrivialConstant.of(irType, 0)
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

    private fun constEvalInitializers(lValueType: ArrayType, expr: InitializerList): NonTrivialConstant {
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

    private fun constEvalInitializers(lValueType: StructType, expr: InitializerList): NonTrivialConstant {
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
            when (val ty = type.cType()) {
                is CStructType -> {
                    val parameters = CallConvention.coerceArgumentTypes(ty)
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

    protected fun irReturnType(retType: TypeDesc): Type = when (val ty = retType.cType()) {
        is VOID -> Type.Void
        is BOOL -> Type.I8
        is CPrimitive -> mb.toIRType<PrimitiveType>(typeHolder, retType.cType())
        is CStructType -> {
            val list = CallConvention.coerceArgumentTypes(ty) ?: return Type.Void
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