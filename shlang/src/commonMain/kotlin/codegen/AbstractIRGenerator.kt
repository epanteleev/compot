package codegen

import common.padTo
import codegen.TypeConverter.toIRLVType
import types.*
import ir.types.*
import ir.value.*
import typedesc.*
import ir.global.*
import parser.nodes.*
import codegen.consteval.*
import ir.module.ExternFunction
import codegen.TypeConverter.toIRType
import ir.attributes.GlobalValueAttribute
import ir.module.builder.impl.ModuleBuilder
import ir.value.constant.*
import ir.value.constant.InitializerListValue
import ir.value.constant.IntegerConstant
import ir.value.constant.NonTrivialConstant
import ir.value.constant.PointerLiteral
import ir.value.constant.PrimitiveConstant
import ir.value.constant.StringLiteralConstant
import parser.LineAgnosticAstPrinter


sealed class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack<Value>,
                                   protected val nameGenerator: NameGenerator) {
    protected fun constEvalExpression(lValueType: NonTrivialType, expr: Expression): NonTrivialConstant? = when (expr) {
        is InitializerList -> when (lValueType) {
            is PrimitiveType -> {
                if (expr.initializers.size != 1) {
                    throw IRCodeGenError("Unsupported initializer list size ${expr.initializers.size}", expr.begin())
                }
                constEvalExpression(lValueType, expr.initializers[0])
            }
            is ArrayType  -> constEvalInitializers(lValueType, expr)
            is StructType -> constEvalInitializers(lValueType, expr)
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
                PointerLiteral.of(mb.addConstant(stringLiteral))
            }
        }
        else -> defaultContEval(lValueType, expr)
    }

    private fun staticAddressOf(expr: Expression): NonTrivialConstant? {
        if (expr !is VarNode) {
            return null
        }
        val value = varStack[expr.name()] ?: return null
        value as GlobalValue
        return PointerLiteral.of(value)
    }

    private fun defaultContEval(lValueType: NonTrivialType, expr: Expression): NonTrivialConstant? {
        val result = constEvalExpression0(expr) ?: return null
        return NonTrivialConstant.of(lValueType, result)
    }

    private fun staticInitializer(expr: Expression): NonTrivialConstant? = when (expr) {
        is UnaryOp -> {
            val operand = staticInitializer(expr.primary) ?: throw IRCodeGenError("Unsupported: $expr", expr.begin())
            when (expr.opType) {
                is PrefixUnaryOpType -> when (expr.opType) {
                    PrefixUnaryOpType.ADDRESS -> operand
                    else -> throw IRCodeGenError("Unsupported unary operator ${expr.opType}", expr.begin())
                }
                else -> throw IRCodeGenError("Unsupported unary operator ${expr.opType}", expr.begin())
            }
        }
        is CompoundLiteral -> {
            val varDesc = expr.typeName.specifyType(typeHolder, listOf())
            val cType = varDesc.type.cType()
            val lValueType = mb.toIRType<AggregateType>(typeHolder,cType)
            val constant = constEvalExpression(lValueType, expr.initializerList) as InitializerListValue
            val gConstant = when (cType) {
                is CArrayType -> {
                    ArrayGlobalConstant(createGlobalConstantName(), constant)
                }
                is CStructType -> {
                    StructGlobalConstant(createGlobalConstantName(), constant)
                }
                else -> throw IRCodeGenError("Unsupported type '$cType', expr='${LineAgnosticAstPrinter.print(expr)}'", expr.begin())
            }
            PointerLiteral.of(mb.addConstant(gConstant))
        }
        is ArrayAccess -> {
            val indexType = expr.expr.resolveType(typeHolder)
            val irType = mb.toIRType<IntegerType>(typeHolder, indexType)
            val index = constEvalExpression(irType, expr.expr) ?: throw IRCodeGenError("Unsupported: $expr", expr.begin())
            val array = staticInitializer(expr.primary) ?: throw IRCodeGenError("Unsupported: $expr", expr.begin())
            array as PointerLiteral
            val gConstant = array.gConstant as GlobalValue
            index as IntegerConstant
            PointerLiteral.of(gConstant, index.toInt())
        }
        is VarNode -> {
            val name = expr.name()
            val value = varStack[name] ?: throw IRCodeGenError("Variable not found: $name", expr.begin())
            PointerLiteral.of(value as GlobalValue)
        }
        is Cast -> {
            val toTypeCast = expr.typeName.specifyType(typeHolder, listOf())
            val lValueType = mb.toIRType<NonTrivialType>(typeHolder, toTypeCast.cType())
            constEvalExpression(lValueType, expr.cast) ?: throw IRCodeGenError("Unsupported: $expr", expr.begin())
        }
        is NumNode -> makeConstant(expr)
        else -> TODO()
    }

    private fun toIrAttribute(storageClass: StorageClass?): GlobalValueAttribute? = when (storageClass) {
        StorageClass.STATIC -> GlobalValueAttribute.INTERNAL
        StorageClass.EXTERN -> null
        else -> GlobalValueAttribute.DEFAULT
    }

    protected fun generateGlobalAssignmentDeclarator(declarator: InitDeclarator): AnyGlobalValue {
        val cType = declarator.cType()
        val lValueType = mb.toIRLVType<NonTrivialType>(typeHolder, cType.type.cType())

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
                    val constant = gc.constant()
                    if (constant.type() != lValueType) {
                        throw IRCodeGenError("Type mismatch: ${gc.constant().type()} != $lValueType", declarator.begin())
                    }
                    val global = mb.addGlobal(declarator.name(), constant, attr)
                    varStack[declarator.name()] = global
                    return global
                }
                is PointerType -> {
                    if (lValueType != constant.type()) {
                        throw IRCodeGenError("Type mismatch: ${constant.type()} != $lValueType", declarator.begin())
                    }
                    val global = mb.addGlobal(declarator.name(), constant, attr)
                    varStack[declarator.name()] = global
                    return global
                }
                else -> throw IRCodeGenError("Unsupported type $lValueType", declarator.begin())
            }
            is PrimitiveConstant -> {
                if (lValueType != constant.type()) {
                    throw IRCodeGenError("Type mismatch: ${constant.type()} != $lValueType", declarator.begin())
                }
                val g = mb.addGlobal(declarator.name(), constant, attr)
                varStack[declarator.name()] = g
                return g
            }
            is InitializerListValue -> when (lValueType) {
                is ArrayType -> {
                    if (lValueType != constant.type()) {
                        throw IRCodeGenError("Type mismatch: ${constant.type()} != $lValueType", declarator.begin())
                    }
                    val global = mb.addGlobal(declarator.name(), constant, attr)
                    varStack[declarator.name()] = global
                    return global
                }
                is StructType -> {
                    if (lValueType != constant.type()) {
                        throw IRCodeGenError("Type mismatch: ${constant.type()} != $lValueType", declarator.begin())
                    }
                    val global = mb.addGlobal(declarator.name(), constant, attr)
                    varStack[declarator.name()] = global
                    return global
                }
                else -> throw IRCodeGenError("Unsupported type $lValueType", declarator.begin())
            }
            is StringLiteralConstant -> {
                val cArrayType = cType.type.cType() as CArrayType
                val newConstant = if (cArrayType.dimension > constant.data().length.toLong()) {
                    val content = constant.content.padTo(cArrayType.dimension.toInt(), "\\0")
                    StringLiteralConstant(ArrayType(Type.I8, cArrayType.dimension.toInt()), content)
                } else {
                    constant
                }
                val global = mb.addGlobal(declarator.name(), newConstant, attr)
                varStack[declarator.name()] = global
                return global
            }
            else -> TODO("$constant")
        }
        throw IRCodeGenError("Unsupported declarator '$declarator'", declarator.begin())
    }

    private fun getExternFunction(name: String, cPrototype: CFunctionPrototype): ExternFunction {
        val externFunction = mb.findExternFunctionOrNull(name)
        if (externFunction != null) {
            println("Warning: extern function $name already exists") //TODO implement warning mechanism
            return externFunction
        }

        return mb.createExternFunction(name, cPrototype.returnType, cPrototype.argumentTypes, cPrototype.attributes)
    }

    private fun makeGlobalValue(name: String, type: VarDescriptor): Value {
        val irType = mb.toIRLVType<NonTrivialType>(typeHolder, type.type.cType())
        val value = if (type.storageClass == StorageClass.EXTERN) {
            mb.addExternValue(name, irType)
        } else {
            val constant = Constant.of(irType, 0)
            mb.addGlobal(name, constant)
        }
        varStack[name] = value
        return value
    }

    protected fun makeConstant(numNode: NumNode) = when (val num = numNode.number.toNumberOrNull()) {
        is Byte   -> I8Value(num.toByte())
        is UByte  -> U8Value(num.toByte())
        is Int    -> I32Value(num.toInt())
        is UInt   -> U32Value(num.toInt())
        is Long   -> I64Value(num.toLong())
        is ULong  -> U64Value(num.toLong())
        is Float  -> F32Value(num.toFloat())
        is Double -> F64Value(num.toDouble())
        else -> throw IRCodeGenError("Unknown number type, num=${numNode.number.str()}", numNode.begin())
    }

    protected fun generateGlobalDeclarator(declarator: Declarator): Value {
        val name = declarator.name()
        val varDesc = declarator.cType()
        when (val type = varDesc.type.cType()) {
            is CFunctionType -> {
                val abstrType = type.functionType
                val cPrototype = CFunctionPrototypeBuilder(declarator.begin(), abstrType, mb, typeHolder).build()
                val externFunc = getExternFunction(name, cPrototype)
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
                val global = mb.addGlobal(name, constant, attr)
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
                val global = mb.addGlobal(name, constant)
                varStack[name] = global
                return global
            }
            is CUncompletedArrayType -> {
                return Value.UNDEF //TODO
            }
            else -> throw IRCodeGenError("Function or struct expected, but was '$type'", declarator.begin())
        }
        throw IRCodeGenError("Unsupported declarator $declarator", declarator.begin())
    }

    protected fun constEvalExpression0(expr: Expression): Number? = when (val ty = expr.resolveType(typeHolder)) {
        INT, SHORT, CHAR, UINT, USHORT, UCHAR, is CEnumType -> {
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
            return constEvalExpression(lValueType, expr.initializers[0]) ?:
                throw IRCodeGenError("Unsupported type $lValueType, expr=${LineAgnosticAstPrinter.print(expr)}", expr.begin())
        }

        val elements = expr.initializers.mapIndexed { index, initializer ->
            val elementLValueType = lValueType.field(index)
            constEvalExpression(elementLValueType, initializer) ?: let {
                throw IRCodeGenError("Unsupported type $elementLValueType, initializer=${LineAgnosticAstPrinter.print(initializer)}", expr.begin())
            }
        }

        return InitializerListValue(lValueType, elements)
    }

    private fun constEvalInitializers(lValueType: StructType, expr: InitializerList): NonTrivialConstant {
        val elements = expr.initializers.mapIndexed { index, initializer ->
            val elementLValueType = lValueType.field(index)
            constEvalExpression(elementLValueType, initializer) ?: let {
                throw IRCodeGenError("Unsupported type $elementLValueType, initializer=${LineAgnosticAstPrinter.print(initializer)}", expr.begin())
            }
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