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
import common.assertion
import ir.attributes.GlobalValueAttribute
import ir.module.builder.impl.ModuleBuilder
import ir.value.constant.*
import parser.LineAgnosticAstPrinter


sealed class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack<Value>,
                                   protected val nameGenerator: NameGenerator) {
    private fun constEvalExpression(lValueType: NonTrivialType, expr: Expression): NonTrivialConstant? = when (expr) {
        is InitializerList -> when (lValueType) {
            is ArrayType  -> constEvalInitializers(lValueType, expr)
            is StructType -> constEvalInitializers(lValueType, expr)
            else -> throw IRCodeGenError("Unsupported initializer list size ${expr.initializers.size}", expr.begin())
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
            is ArrayType -> StringLiteralConstant(ArrayType(Type.I8, expr.length()) ,expr.data())
            else -> {
                val constant = expr.data()
                val stringLiteral = StringLiteralGlobalConstant(createStringLiteralName(), ArrayType(Type.U8, expr.length()), constant)
                PointerLiteral.of(mb.addConstant(stringLiteral))
            }
        }
        is Cast -> {
            val type = expr.resolveType(typeHolder)
            if (type is CPointer) {
                constEvalExpression(lValueType, expr.cast) ?: throw IRCodeGenError("Unsupported: $expr", expr.begin())
            } else {
                defaultContEval(lValueType, expr)
            }
        }
        is VarNode -> {
            // TODO Code duplication!!!
            val name = expr.name()
            val value = varStack[name]
                ?: typeHolder.findEnumByEnumerator(name)
                ?: throw IRCodeGenError("Variable not found: $name", expr.begin())

            val cType = expr.resolveType(typeHolder)
            if (cType is CPointer || cType is CStringLiteral) {
                PointerLiteral.of(value as GlobalValue)
            } else {
                defaultContEval(lValueType, expr)
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

    private fun staticInitializer(expr: Expression): NonTrivialConstant = when (expr) {
        is UnaryOp -> {
            val operand = staticInitializer(expr.primary)
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
            val array = staticInitializer(expr.primary)
            array as PointerLiteral
            val gConstant = array.gConstant as GlobalValue
            index as IntegerConstant
            PointerLiteral.of(gConstant, index.toInt())
        }
        is VarNode -> {
            // TODO Code duplication!!!
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
        val manglingName = generateName(declarator, cType)
        assertion(cType.storageClass != StorageClass.EXTERN) { "invariant: cType=$cType" }
        val attr = toIrAttribute(cType.storageClass)!!

        val constEvalResult = constEvalExpression(lValueType, declarator.rvalue) ?: staticInitializer(declarator.rvalue)
        when (constEvalResult) {
            is PointerLiteral -> when (lValueType) {
                is ArrayType, is PointerType -> {
                    val global = mb.addGlobalValue(manglingName, constEvalResult, attr)
                    varStack[declarator.name()] = global
                    return global
                }
                else -> throw IRCodeGenError("Unsupported type $lValueType", declarator.begin())
            }
            is PrimitiveConstant -> {
                if (lValueType != constEvalResult.type()) {
                    throw IRCodeGenError("Type mismatch: ${constEvalResult.type()} != $lValueType", declarator.begin())
                }
                val g = mb.addGlobalValue(manglingName, constEvalResult, attr)
                varStack[declarator.name()] = g
                return g
            }
            is InitializerListValue -> when (lValueType) {
                is ArrayType -> {
                    if (lValueType != constEvalResult.type()) {
                        throw IRCodeGenError("Type mismatch: ${constEvalResult.type()} != $lValueType", declarator.begin())
                    }
                    val global = mb.addGlobalValue(manglingName, constEvalResult, attr)
                    varStack[declarator.name()] = global
                    return global
                }
                is StructType -> {
                    if (lValueType != constEvalResult.type()) {
                        throw IRCodeGenError("Type mismatch: ${constEvalResult.type()} != $lValueType", declarator.begin())
                    }
                    val global = mb.addGlobalValue(manglingName, constEvalResult, attr)
                    varStack[declarator.name()] = global
                    return global
                }
                else -> throw IRCodeGenError("Unsupported type $lValueType", declarator.begin())
            }
            is StringLiteralConstant -> {
                val dimension = when (val cArrayType = cType.type.cType() as AnyCArrayType) {
                    is CArrayType     -> cArrayType.dimension
                    is CStringLiteral -> cArrayType.dimension
                    else -> throw IRCodeGenError("Unsupported type $lValueType", declarator.begin())
                }
                val newConstant = if (dimension > constEvalResult.toString().length.toLong()) {
                    val content = constEvalResult.content.padTo(dimension.toInt(), "\\0")
                    StringLiteralConstant(ArrayType(Type.I8, dimension.toInt()), content)
                } else {
                    constEvalResult
                }
                val global = mb.addGlobalValue(manglingName, newConstant, attr)
                varStack[declarator.name()] = global
                return global
            }
        }
    }

    private fun getExternFunction(name: String, cPrototype: CFunctionPrototype): ExternFunction {
        val externFunction = mb.findExternFunctionOrNull(name)
        if (externFunction != null) {
            println("Warning: extern function $name already exists") //TODO implement warning mechanism
            return externFunction
        }

        return mb.createExternFunction(name, cPrototype.returnType, cPrototype.argumentTypes, cPrototype.attributes)
    }

    private fun makeGlobalValue(realName: String, manglingName: String, type: VarDescriptor): Value {
        val irType = mb.toIRLVType<NonTrivialType>(typeHolder, type.type.cType())
        val value = if (type.storageClass == StorageClass.EXTERN) {
            mb.addExternValue(manglingName, irType)
        } else {
            val constant = NonTrivialConstant.of(irType, 0)
            mb.addGlobalValue(manglingName, constant)
        }
        varStack[realName] = value
        return value
    }

    protected fun makeConstant(numNode: NumNode) = when (val num = numNode.number.toNumberOrNull()) {
        is Byte   -> I8Value(num.toByte())
        is Int    -> I32Value(num.toInt())
        is Long   -> I64Value(num.toLong())
        is Float  -> F32Value(num.toFloat())
        is Double -> F64Value(num.toDouble())
        else -> throw IRCodeGenError("Unknown number type, num=${numNode.number.str()}", numNode.begin())
    }

    private fun generateName(declarator: AnyDeclarator, typeDesc: VarDescriptor): String { //TODO generateName in VarDescriptor class
        return if (typeDesc.storageClass == StorageClass.STATIC) {
            nameGenerator.createStaticVariableName(declarator.name())
        } else {
            declarator.name()
        }
    }

    protected fun generateGlobalDeclarator(declarator: Declarator): Value {
        val  varDesc = declarator.cType()
        val manglingName = generateName(declarator,  varDesc)
        when (val cType = varDesc.type.cType()) {
            is CFunctionType -> {
                val cPrototype = CFunctionPrototypeBuilder(declarator.begin(), cType.functionType, mb, typeHolder, varDesc.storageClass).build()
                val externFunc = getExternFunction(declarator.name(), cPrototype) //TODO extern not everytime
                varStack[declarator.name()] = externFunc
                return externFunc
            }
            is CPrimitive -> {
                return makeGlobalValue(declarator.name(), manglingName, varDesc)
            }
            is CArrayType -> {
                val irType = mb.toIRType<ArrayType>(typeHolder, cType)
                if (varDesc.storageClass == StorageClass.EXTERN) {
                    val externValue = mb.addExternValue(manglingName, irType)
                    varStack[declarator.name()] = externValue
                    return externValue
                }
                val attr = toIrAttribute(varDesc.storageClass)!!
                val zero = NonTrivialConstant.of(irType.elementType(), 0)
                val elements = generateSequence { zero }.take(irType.length).toList()
                val constant = InitializerListValue(irType, elements)
                val global = mb.addGlobalValue(manglingName, constant, attr)
                varStack[declarator.name()] = global
                return global
            }
            is CStructType -> {
                val irType = mb.toIRType<StructType>(typeHolder, cType)
                if (varDesc.storageClass == StorageClass.EXTERN) {
                    val externValue = mb.addExternValue(manglingName, irType)
                    varStack[declarator.name()] = externValue
                    return externValue
                }
                val elements = arrayListOf<NonTrivialConstant>()
                for (field in cType.fields()) {
                    val irFieldType = mb.toIRType<NonTrivialType>(typeHolder, field.cType())
                    val zero = NonTrivialConstant.of(irFieldType, 0)
                    elements.add(zero)
                }
                val constant = InitializerListValue(irType, elements)
                val global = mb.addGlobalValue(manglingName, constant)
                varStack[declarator.name()] = global
                return global
            }
            is CUncompletedArrayType -> return UndefValue //TODO
            else -> throw IRCodeGenError("Function or struct expected, but was '$cType'", declarator.begin())
        }
    }

    protected fun constEvalExpression0(expr: Expression): Number? = when (expr.resolveType(typeHolder)) {
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

    private fun createGlobalConstantName(): String {
        return nameGenerator.createGlobalConstantName()
    }
}