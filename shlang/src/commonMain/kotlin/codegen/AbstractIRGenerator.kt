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


internal sealed class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack<Value>,
                                   protected val nameGenerator: NameGenerator) {
    private fun constEvalExpression(lValueType: NonTrivialType, expr: Expression): NonTrivialConstant? = when (expr) {
        is InitializerList -> when (lValueType) {
            is AggregateType -> constEvalInitializers(lValueType, expr)
            else -> throw IRCodeGenError("Unsupported initializer list size ${expr.initializers.size}", expr.begin())
        }
        is UnaryOp -> {
            if (expr.opType == PrefixUnaryOpType.ADDRESS) {
                val nonTrivialType = expr.primary.resolveType(typeHolder)
                val irType         = mb.toIRLVType<NonTrivialType>(typeHolder, nonTrivialType)

                staticAddressOf(expr.primary) ?: constEvalExpression(irType, expr.primary)
                    ?: throw IRCodeGenError("cannon evaluate", expr.primary.begin())
            } else {
                defaultContEval(lValueType, expr)
            }
        }
        is SingleInitializer -> constEvalExpression(lValueType, expr.expr)
        is StringNode        -> when (lValueType) {
            is ArrayType -> StringLiteralConstant(ArrayType(I8Type, expr.length()) ,expr.data())
            else -> {
                val constant = expr.data()
                val stringLiteral = StringLiteralGlobalConstant(createStringLiteralName(), ArrayType(U8Type, expr.length()), constant)
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
        is VarNode -> when (expr.resolveType(typeHolder)) {
            is CAggregateType, is AnyCFunctionType -> staticAddressOf(expr) ?: throw RuntimeException("internal error")
            else -> defaultContEval(lValueType, expr)
        }
        is CompoundLiteral -> {
            val varDesc = expr.typeName.specifyType(typeHolder, listOf())
            val cType = varDesc.typeDesc.cType()
            val typenameType = mb.toIRType<AggregateType>(typeHolder, cType)
            val constant = constEvalExpression(typenameType, expr.initializerList) as InitializerListValue
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

            val primaryCType = expr.primary.resolveType(typeHolder)
            val primaryIrType = mb.toIRType<NonTrivialType>(typeHolder, primaryCType)

            val array = constEvalExpression(primaryIrType, expr.primary)
            array as PointerLiteral
            val gConstant = array.gConstant as GlobalValue
            index as IntegerConstant
            PointerLiteral.of(gConstant, index.toInt())
        }
        else -> defaultContEval(lValueType, expr)
    }

    private fun getRValueVariable(varNode: VarNode): Value {
        val name = varNode.name()
        val rvalueAttr = varStack[name]
        if (rvalueAttr != null) {
            return rvalueAttr
        }
        val global = mb.findFunction(name)
        if (global != null) {
            return global
        }

        val enumValue = typeHolder.findEnumByEnumerator(name)
        if (enumValue != null) {
            return I32Value.of(enumValue)
        }

        throw IRCodeGenError("Variable '$name' not found", varNode.begin())
    }

    private fun staticAddressOf(expr: Expression): NonTrivialConstant? {
        if (expr !is VarNode) {
            return null
        }
        val value = getRValueVariable(expr)
        return PointerLiteral.of(value.asValue())
    }

    private fun defaultContEval(lValueType: NonTrivialType, expr: Expression): NonTrivialConstant? {
        val result = constEvalExpression0(expr) ?: return null
        return NonTrivialConstant.of(lValueType, result)
    }

    private fun toIrAttribute(storageClass: StorageClass?): GlobalValueAttribute = when (storageClass) {
        StorageClass.STATIC -> GlobalValueAttribute.INTERNAL
        StorageClass.EXTERN -> throw RuntimeException("invariant")
        else -> GlobalValueAttribute.DEFAULT
    }

    protected fun generateGlobalAssignmentDeclarator(declarator: InitDeclarator): AnyGlobalValue {
        val varDescriptor = declarator.varDescriptor()
        val lValueType = mb.toIRLVType<NonTrivialType>(typeHolder, varDescriptor.typeDesc.cType())
        assertion(varDescriptor.storageClass != StorageClass.EXTERN) { "invariant: cType=$varDescriptor" }
        val attr = toIrAttribute(varDescriptor.storageClass)

        val constEvalResult = constEvalExpression(lValueType, declarator.rvalue)
            ?: throw IRCodeGenError("Cannon evaluate expression", declarator.rvalue.begin())

        when (constEvalResult) {
            is PointerLiteral -> when (lValueType) {
                is ArrayType, is PtrType -> {
                    return registerGlobal(declarator, constEvalResult, attr)
                }
                else -> throw IRCodeGenError("Unsupported type $lValueType", declarator.begin())
            }
            is PrimitiveConstant -> {
                if (lValueType != constEvalResult.type()) {
                    throw IRCodeGenError("Type mismatch: ${constEvalResult.type()} != $lValueType", declarator.begin())
                }
                return registerGlobal(declarator, constEvalResult, attr)
            }
            is InitializerListValue -> {
                if (lValueType != constEvalResult.type()) {
                    throw IRCodeGenError("Type mismatch: ${constEvalResult.type()} != $lValueType", declarator.begin())
                }
                return when (lValueType) {
                    is AggregateType -> registerGlobal(declarator, constEvalResult, attr)
                    else -> throw IRCodeGenError("Unsupported type $lValueType", declarator.begin())
                }
            }
            is StringLiteralConstant -> {
                val dimension = when (val cArrayType = varDescriptor.typeDesc.cType() as AnyCArrayType) {
                    is CArrayType     -> cArrayType.dimension
                    is CStringLiteral -> cArrayType.dimension
                    is CUncompletedArrayType -> throw IRCodeGenError("Uncompleted array type", declarator.begin())
                }
                val newConstant = if (dimension > constEvalResult.toString().length.toLong()) {
                    val content = constEvalResult.content.padTo(dimension.toInt(), "\\0")
                    StringLiteralConstant(ArrayType(I8Type, dimension.toInt()), content)
                } else {
                    constEvalResult
                }
                return registerGlobal(declarator, newConstant, attr)
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

    protected fun makeConstant(numNode: NumNode) = when (val num = numNode.number.toNumberOrNull()) {
        is Byte   -> I8Value.of(num.toByte())
        is Int    -> I32Value.of(num.toInt())
        is Long   -> I64Value.of(num.toLong())
        is Float  -> F32Value(num.toFloat())
        is Double -> F64Value(num.toDouble())
        else -> throw IRCodeGenError("Unknown number type, num=${numNode.number.str()}", numNode.begin())
    }

    private fun generateName(declarator: AnyDeclarator): String {
        val varDesc = declarator.varDescriptor()
        return if (varDesc.storageClass == StorageClass.STATIC) {
            nameGenerator.createStaticVariableName(declarator.name())
        } else {
            declarator.name()
        }
    }

    private fun registerExtern(declarator: Declarator, irType: NonTrivialType): ExternValue {
        val externValue = mb.addExternValue(generateName(declarator), irType)
        varStack[declarator.name()] = externValue
        return externValue
    }

    private fun registerGlobal(declarator: AnyDeclarator, cst: NonTrivialConstant, attribute: GlobalValueAttribute): GlobalValue {
        val has = varStack[declarator.name()]
        if (has == null) {
            val globalValue = mb.addGlobalValue(generateName(declarator), cst, attribute)
            varStack[declarator.name()] = globalValue
            return globalValue
        }
        if (has !is AnyGlobalValue) {
            throw IRCodeGenError("Variable '${declarator.name()}' isn't global", declarator.begin())
        }
        val globalValue = mb.redefineGlobalValue(has, has.name(), cst, attribute)
        varStack[declarator.name()] = globalValue
        return globalValue
    }

    protected fun generateGlobalDeclarator(declarator: Declarator): Value {
        val varDesc = declarator.varDescriptor()
        when (val cType = varDesc.typeDesc.cType()) {
            is CFunctionType -> {
                val cPrototype = CFunctionPrototypeBuilder(declarator.begin(), cType.functionType, mb, typeHolder, varDesc.storageClass).build()
                val externFunc = getExternFunction(declarator.name(), cPrototype) //TODO extern not everytime
                varStack[declarator.name()] = externFunc
                return externFunc
            }
            is CPrimitive -> {
                val irType = mb.toIRLVType<NonTrivialType>(typeHolder, cType)
                if (varDesc.storageClass == StorageClass.EXTERN) {
                    return registerExtern(declarator, irType)
                }

                val attr = toIrAttribute(varDesc.storageClass)
                val constant = NonTrivialConstant.of(irType, 0)
                return registerGlobal(declarator, constant, attr)
            }
            is CArrayType -> {
                val irType = mb.toIRType<ArrayType>(typeHolder, cType)
                if (varDesc.storageClass == StorageClass.EXTERN) {
                    return registerExtern(declarator, irType)
                }

                val attr = toIrAttribute(varDesc.storageClass)
                val zero = NonTrivialConstant.of(irType.elementType(), 0)
                val elements = generateSequence { zero }.take(irType.length).toList()
                return registerGlobal(declarator, InitializerListValue(irType, elements), attr)
            }
            is CStructType -> {
                val irType = mb.toIRType<StructType>(typeHolder, cType)
                if (varDesc.storageClass == StorageClass.EXTERN) {
                    return registerExtern(declarator, irType)
                }

                val elements = arrayListOf<NonTrivialConstant>()
                for (field in cType.members()) {
                    val irFieldType = mb.toIRType<NonTrivialType>(typeHolder, field.cType())
                    val zero = NonTrivialConstant.of(irFieldType, 0)
                    elements.add(zero)
                }
                val attr = toIrAttribute(varDesc.storageClass)
                return registerGlobal(declarator, InitializerListValue(irType, elements), attr)
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

    private fun constEvalInitializers(lValueType: AggregateType, expr: InitializerList): NonTrivialConstant {
        if (expr.initializers.size == 1 && lValueType is ArrayType && lValueType.elementType() is I8Type) {
            // char[] = {"string"} pattern
            return constEvalExpression(lValueType, expr.initializers[0]) ?:
                throw IRCodeGenError("Unsupported type $lValueType, expr=${LineAgnosticAstPrinter.print(expr)}", expr.begin())
        }

        val elements = arrayListOf<NonTrivialConstant>()
        for (i in expr.initializers.indices) {
            elements.add(NonTrivialConstant.of(lValueType.field(i), 0))
        }

        for ((index, initializer) in expr.initializers.withIndex()) {
            when (initializer) {
                is SingleInitializer -> {
                    val elementLValueType = lValueType.field(index)
                    val result = constEvalExpression(elementLValueType, initializer) ?: let {
                        throw IRCodeGenError("Unsupported type $elementLValueType, initializer=${LineAgnosticAstPrinter.print(initializer)}", expr.begin())
                    }

                    elements[index] = result
                }
                is DesignationInitializer -> {
                    if (lValueType !is StructType) {
                        throw IRCodeGenError("Unsupported type $lValueType, initializer=${LineAgnosticAstPrinter.print(initializer)}", expr.begin())
                    }

                    val elementLValueType = lValueType.field(index)
                    val result = constEvalExpression(elementLValueType, initializer) ?: let {
                        throw IRCodeGenError("Unsupported type $elementLValueType, initializer=${LineAgnosticAstPrinter.print(initializer)}", expr.begin())
                    }

                    elements[index] = result
                }
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