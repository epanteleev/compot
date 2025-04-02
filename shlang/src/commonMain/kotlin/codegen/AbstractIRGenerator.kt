package codegen

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
import ir.module.DirectFunctionPrototype
import ir.module.builder.impl.ModuleBuilder
import ir.value.constant.*
import parser.LineAgnosticAstPrinter


internal sealed class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                          protected val typeHolder: TypeHolder,
                                          protected val vregStack: VarStack<Value>,
                                          protected val nameGenerator: NameGenerator) {

    private fun constEvalInitializerList(lValueType: CType, list: InitializerList): NonTrivialConstant = when (lValueType) {
        is CAggregateType -> constEvalInitializers(lValueType, list)
        else -> throw IRCodeGenError("Unsupported initializer list size ${list.initializers.size}", list.begin())
    }

    private fun constEvalInitializer(lValueType: CType, initializer: Initializer): NonTrivialConstant? = when (initializer) {
        is InitializerListInitializer -> constEvalInitializerList(lValueType, initializer.list)
        is ExpressionInitializer -> constEvalInitializerList(lValueType, initializer.expr)
    }

    private fun constEvalInitializerList(lValueType: CType, expr: Expression): NonTrivialConstant? = when (expr) {
        is UnaryOp -> {
            if (expr.opType == PrefixUnaryOpType.ADDRESS) {
                val nonTrivialType = expr.primary.resolveType(typeHolder)

                staticAddressOf(expr.primary) ?: constEvalInitializerList(nonTrivialType, expr.primary)
                    ?: throw IRCodeGenError("cannon evaluate", expr.primary.begin())
            } else {
                defaultContEval(lValueType, expr)
            }
        }
        is StringNode -> when (lValueType) {
            is AnyCArrayType -> {
                val dimension = when (lValueType) {
                    is CArrayType -> lValueType.dimension.toInt()
                    is CStringLiteral -> lValueType.dimension.toInt()
                    is CUncompletedArrayType -> expr.length()
                }
                StringLiteralConstant(ArrayType(I8Type, dimension), expr.data())
            }
            is CPointer -> {
                val stringLiteral = StringLiteralGlobalConstant(createStringLiteralName(), ArrayType(U8Type, expr.length()), expr.data())
                PointerLiteral.of(mb.addConstant(stringLiteral))
            }
            else -> throw IRCodeGenError("Unsupported type $lValueType", expr.begin())
        }
        is Cast -> {
            val type = expr.resolveType(typeHolder)
            if (type is CPointer) {
                constEvalInitializerList(lValueType, expr.cast) ?: throw IRCodeGenError("Unsupported: $expr", expr.begin())
            } else {
                defaultContEval(lValueType, expr)
            }
        }
        is VarNode -> when (expr.resolveType(typeHolder)) {
            is CAggregateType, is AnyCFunctionType -> staticAddressOf(expr) ?: throw RuntimeException("internal error")
            else -> defaultContEval(lValueType, expr)
        }
        is CompoundLiteral -> {
            val varDesc = expr.typeName.specifyType(typeHolder)
            val cType = varDesc.typeDesc.cType()
            val constant = constEvalInitializerList(cType, expr.initializerList) as InitializerListValue
            val gConstant = when (cType) {
                is CArrayType  -> ArrayGlobalConstant(createGlobalConstantName(), constant)
                is CStructType -> StructGlobalConstant(createGlobalConstantName(), constant)
                else -> throw IRCodeGenError("Unsupported type '$cType', expr='${LineAgnosticAstPrinter.print(expr)}'", expr.begin())
            }
            PointerLiteral.of(mb.addConstant(gConstant))
        }
        is ArrayAccess -> {
            val indexType = expr.expr.resolveType(typeHolder)
            val index = constEvalInitializerList(indexType, expr.expr) ?: throw IRCodeGenError("Unsupported: $expr", expr.begin())

            val primaryCType = expr.primary.resolveType(typeHolder)
            val array = constEvalInitializerList(primaryCType, expr.primary)
            array as PointerLiteral
            val gConstant = array.gConstant as GlobalValue
            index as IntegerConstant
            PointerLiteral.of(gConstant, index.toInt())
        }
        else -> defaultContEval(lValueType, expr)
    }

    private fun getRValueVariable(varNode: VarNode): Value {
        val name = varNode.name()
        val rvalueAttr = vregStack[name]
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

    private fun defaultContEval(lValueCType: CType, expr: Expression): NonTrivialConstant? {
        val result = constEvalExpression0(expr) ?: return null
        val lValueType = mb.toIRLVType<NonTrivialType>(typeHolder, lValueCType)
        return NonTrivialConstant.of(lValueType, result)
    }

    private fun toIrAttribute(storageClass: StorageClass?): GlobalValueAttribute = when (storageClass) {
        StorageClass.STATIC -> GlobalValueAttribute.INTERNAL
        StorageClass.EXTERN -> throw RuntimeException("invariant")
        else -> GlobalValueAttribute.DEFAULT
    }

    protected fun generateGlobalAssignmentDeclarator(varDescriptor: VarDescriptor, declarator: InitDeclarator): AnyGlobalValue {
        val lValueCType = varDescriptor.cType()
        val attr = toIrAttribute(varDescriptor.storageClass)

        val constEvalResult = constEvalInitializer(lValueCType, declarator.rvalue)
            ?: throw IRCodeGenError("Cannon evaluate expression", declarator.rvalue.begin())

        return registerGlobal(declarator, constEvalResult, attr)
    }

    private fun getExternFunction(name: String, cPrototype: CFunctionPrototype): ExternFunction {
        val externFunction = mb.findExternFunctionOrNull(name)
        if (externFunction != null) {
            return externFunction
        }

        return mb.createExternFunction(name, cPrototype.returnType, cPrototype.argumentTypes, cPrototype.attributes)
    }

    private fun createFunctionPrototype(declarator: Declarator, cPrototype: CFunctionPrototype): DirectFunctionPrototype {
        val existed = mb.findFunctionDeclarationOrNull(declarator.name())
            ?: return mb.createFunctionDeclaration(declarator.name(), cPrototype.returnType, cPrototype.argumentTypes, cPrototype.attributes)

        if (existed.returnType() != cPrototype.returnType) {
            throw IRCodeGenError("Function '${declarator.name()}' already exists with different return type", declarator.begin())
        }

        if (existed.arguments() != cPrototype.argumentTypes) {
            throw IRCodeGenError("Function '${declarator.name()}' already exists with different arguments", declarator.begin())
        }

        return existed
    }

    private fun generateName(declarator: AnyDeclarator): String {
        val varDesc = typeHolder.getVarTypeOrNull(declarator.name())
            ?: throw IRCodeGenError("Variable '${declarator.name()}' not found", declarator.begin())

        return if (varDesc.storageClass == StorageClass.STATIC) {
            nameGenerator.createStaticVariableName(declarator.name())
        } else {
            declarator.name()
        }
    }

    private fun registerExtern(declarator: Declarator, cType: CType): ExternValue {
        val existed = vregStack[declarator.name()]
        if (existed != null) {
            if (existed !is ExternValue) {
                throw IRCodeGenError("Variable '${declarator.name()}' already exists", declarator.begin())
            }
            val type = typeHolder.getVarTypeOrNull(declarator.name())
                ?: throw IRCodeGenError("Variable '${declarator.name()}' not found", declarator.begin())

            if (type.storageClass != StorageClass.EXTERN) {
                throw IRCodeGenError("Variable '${declarator.name()}' already exists with different storage class", declarator.begin())
            }
            if (type.cType() != cType) {
                throw IRCodeGenError("Variable '${declarator.name()}' already exists with different type", declarator.begin())
            }

            return existed
        }

        val irType = mb.toIRLVType<NonTrivialType>(typeHolder, cType)
        val externValue = mb.addExternValue(generateName(declarator), irType) ?:
            throw IRCodeGenError("Variable '${declarator.name()}' already exists", declarator.begin())

        vregStack[declarator.name()] = externValue
        return externValue
    }

    private fun registerGlobal(declarator: AnyDeclarator, cst: NonTrivialConstant, attribute: GlobalValueAttribute): GlobalValue {
        val has = vregStack[declarator.name()]
        if (has == null) {
            val globalValue = mb.addGlobalValue(generateName(declarator), cst, attribute)
            vregStack[declarator.name()] = globalValue
            return globalValue
        }
        if (has !is AnyGlobalValue) {
            throw IRCodeGenError("Variable '${declarator.name()}' isn't global", declarator.begin())
        }
        val globalValue = mb.redefineGlobalValue(has, has.name(), cst, attribute)
        vregStack[declarator.name()] = globalValue
        return globalValue
    }

    protected fun generateExternDeclarator(varDesc: VarDescriptor, declarator: Declarator): Value =
        when (val cType = varDesc.cType()) {
            is CFunctionType -> {
                val cPrototype = CFunctionPrototypeBuilder(declarator.begin(), cType.functionType, mb, typeHolder, varDesc.storageClass)
                    .build()
                getExternFunction(declarator.name(), cPrototype)
            }
            else -> registerExtern(declarator, cType)
        }

    protected fun generateGlobalDeclarator(varDesc: VarDescriptor, declarator: Declarator): Value = when (val cType = varDesc.cType()) {
        is CFunctionType -> {
            val cPrototype = CFunctionPrototypeBuilder(declarator.begin(), cType.functionType, mb, typeHolder, varDesc.storageClass).build()
            createFunctionPrototype(declarator, cPrototype)
        }
        is CPrimitive -> {
            val irType = mb.toIRLVType<PrimitiveType>(typeHolder, cType)
            val attr = toIrAttribute(varDesc.storageClass)
            val constant = PrimitiveConstant.of(irType, 0)
            registerGlobal(declarator, constant, attr)
        }
        is CArrayType -> {
            val irType = mb.toIRType<ArrayType>(typeHolder, cType)
            val attr = toIrAttribute(varDesc.storageClass)
            val zero = NonTrivialConstant.of(irType.elementType(), 0)
            val elements = generateSequence { zero }.take(irType.length).toList()
            registerGlobal(declarator, InitializerListValue(irType, elements), attr)
        }
        is AnyCStructType -> {
            val irType = mb.toIRType<StructType>(typeHolder, cType)
            val elements = arrayListOf<NonTrivialConstant>()
            for (field in irType.fields) {
                elements.add(NonTrivialConstant.of(field, 0))
            }
            val attr = toIrAttribute(varDesc.storageClass)
            registerGlobal(declarator, InitializerListValue(irType, elements), attr)
        }
        is CUncompletedArrayType -> {
            // Important: gcc & clang allow to declare array with 0 element
            val irType = ArrayType(mb.toIRType<NonTrivialType>(typeHolder, cType.element().cType()), 1)
            val attr = toIrAttribute(varDesc.storageClass)
            val zero = NonTrivialConstant.of(irType.elementType(), 0)
            val elements = arrayListOf(zero)
            registerGlobal(declarator, InitializerListValue(irType, elements), attr)
        }
        is AbstractCFunction -> TODO()
        is CStringLiteral -> TODO()
    }

    protected fun constEvalExpression0(expr: Expression): Number? = ConstEvalExpression.eval(expr, typeHolder)

    private fun initialConstants(lValueCType: CAggregateType, initializerList: InitializerList): MutableList<NonTrivialConstant> {
        val lValueType = mb.toIRType<AggregateType>(typeHolder, lValueCType)
        val elements = arrayListOf<NonTrivialConstant>()
        when (lValueCType) {
            is AnyCArrayType -> {
                for (i in initializerList.initializers.indices) {
                    elements.add(NonTrivialConstant.of(lValueType.field(i), 0))
                }

                return elements
            }
            is AnyCStructType -> {
                for (i in initializerList.initializers.indices) {
                    elements.add(NonTrivialConstant.of(lValueType.field(i), 0))
                }

                return elements
            }
        }
    }

    private fun constEvalInitializers(lValueCType: CAggregateType, expr: InitializerList): NonTrivialConstant {
        if (expr.initializers.size == 1 && lValueCType is CArrayType && lValueCType.element().cType() is CHAR) {
            // char[] = {"string"} pattern
            val singleInitializer = expr.initializers[0]
            if (singleInitializer !is SingleInitializer) {
                throw IRCodeGenError("Unsupported initializer list size ${expr.initializers.size}", expr.begin())
            }

            return constEvalInitializer(lValueCType, singleInitializer.expr) ?:
                throw IRCodeGenError("Unsupported type $lValueCType, expr=${LineAgnosticAstPrinter.print(expr)}", expr.begin())
        }

        val lValueType = mb.toIRType<AggregateType>(typeHolder, lValueCType)
        val elements = initialConstants(lValueCType, expr)

        for ((index, initializer) in expr.initializers.withIndex()) {
            when (initializer) {
                is SingleInitializer -> {
                    val elementLValueType = when (lValueCType) {
                        is AnyCArrayType -> lValueCType.element().cType()
                        is CStructType -> lValueCType.fieldByIndex(index).cType()
                        is CUnionType -> lValueCType.descriptors().first().cType() // TODO check this
                    }
                    val result = constEvalInitializer(elementLValueType, initializer.expr) ?:
                        throw IRCodeGenError("Unsupported type $elementLValueType, initializer=${LineAgnosticAstPrinter.print(initializer)}", expr.begin())

                    elements[index] = result
                }
                is DesignationInitializer -> {
                    if (lValueCType !is AnyCStructType) {
                        throw IRCodeGenError("Unsupported type $lValueCType, initializer=${LineAgnosticAstPrinter.print(initializer)}", expr.begin())
                    }

                    val lastDesignator = initializer.designation.designators.last() as MemberDesignator //TODO check all designators
                    val nameToAccess = lastDesignator.name()

                    val elementLValueType = lValueCType.fieldByName(nameToAccess).cType()
                    val result = constEvalInitializer(elementLValueType, initializer.initializer) ?:
                        throw IRCodeGenError("Unsupported type $elementLValueType, initializer=${LineAgnosticAstPrinter.print(initializer)}", expr.begin())

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