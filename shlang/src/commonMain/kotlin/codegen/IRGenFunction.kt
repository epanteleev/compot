package codegen

import types.*
import ir.types.*
import ir.value.*
import parser.nodes.*
import common.assertion
import codegen.TypeConverter.coerceArguments
import ir.instruction.*
import ir.module.block.Label
import codegen.TypeConverter.convertToType
import codegen.TypeConverter.toIRLVType
import codegen.TypeConverter.toIRType
import codegen.TypeConverter.toIndexType
import codegen.consteval.CommonConstEvalContext
import codegen.consteval.ConstEvalExpression
import codegen.consteval.TryConstEvalExpressionInt
import intrinsic.VaInit
import intrinsic.VaStart
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE
import ir.attributes.ByValue
import ir.attributes.FunctionAttribute
import ir.attributes.VarArgAttribute
import ir.global.StringLiteralGlobalConstant
import ir.module.AnyFunctionPrototype
import ir.module.IndirectFunctionPrototype
import ir.module.builder.impl.ModuleBuilder
import ir.module.builder.impl.FunctionDataBuilder
import ir.value.constant.*
import parser.LineAgnosticAstPrinter
import parser.nodes.visitors.DeclaratorVisitor
import parser.nodes.visitors.StatementVisitor
import typedesc.*


private class IrGenFunction(moduleBuilder: ModuleBuilder,
                    typeHolder: TypeHolder,
                    varStack: VarStack<Value>,
                    nameGenerator: NameGenerator,
                    private val ir: FunctionDataBuilder,
                    private val functionType: CFunctionType) :
    AbstractIRGenerator(moduleBuilder, typeHolder, varStack, nameGenerator),
    StatementVisitor<Unit>,
    DeclaratorVisitor<Value> {
    private var stringTolabel = mutableMapOf<String, Label>()
    private val stmtStack = StmtStack()
    private val initializerContext = InitializerContext()

    private val vaListIrType by lazy {
        mb.toIRType<StructType>(typeHolder, VaStart.vaList)
    }

    private inline fun<reified T> scoped(noinline block: () -> T): T {
        return typeHolder.scoped { varStack.scoped(block) }
    }

    private fun seekOrAddLabel(name: String): Label {
        return stringTolabel[name] ?: let {
            val newLabel = ir.createLabel()
            stringTolabel[name] = newLabel
            newLabel
        }
    }

    private fun visitDeclaration(declaration: Declaration) {
        declaration.specifyType(typeHolder)

        for (declarator in declaration.declarators()) {
            declarator.accept(this)
        }
    }

    private fun makeConditionFromExpression(condition: Expression): Value {
        val conditionExpr = visitExpression(condition, true)
        if (conditionExpr.type() == Type.U1) {
            return conditionExpr
        }

        return when (val type = conditionExpr.type()) {
            is IntegerType, PointerType -> ir.icmp(conditionExpr, IntPredicate.Ne, Constant.of(type.asType(), 0))
            is FloatingPointType        -> ir.fcmp(conditionExpr, FloatPredicate.One, Constant.of(type, 0))
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private inline fun<reified T: AnyPredicateType> makeCondition(a: Value, predicate: T, b: Value): Value = when (a.type()) {
        is IntegerType, PointerType -> ir.icmp(a, predicate as IntPredicate, b)
        is FloatingPointType        -> ir.fcmp(a, predicate as FloatPredicate, b)
        else -> throw IRCodeGenError("Unknown type")
    }

    private fun visitCompoundLiteral(compoundLiteral: CompoundLiteral): Value {
        val type   = compoundLiteral.typeDesc(typeHolder)
        val irType = mb.toIRType<AggregateType>(typeHolder, type.cType())
        val adr    = ir.alloc(irType)
        initializerContext.scope(adr, type) {
            visitInitializerList(compoundLiteral.initializerList)
        }
        return adr
    }

    private fun visitExpression(expression: Expression, isRvalue: Boolean): Value = when (expression) {
        is BinaryOp     -> visitBinary(expression, isRvalue)
        is UnaryOp      -> visitUnary(expression, isRvalue)
        is NumNode      -> visitNumNode(expression)
        is VarNode      -> visitVarNode(expression, isRvalue)
        is FunctionCall -> visitFunctionCall(expression)
        is Cast         -> visitCast(expression)
        is ArrayAccess  -> visitArrayAccess(expression, isRvalue)
        is StringNode   -> visitStringNode(expression)
        is SizeOf       -> visitSizeOf(expression)
        is MemberAccess -> visitMemberAccess(expression, isRvalue)
        is ArrowMemberAccess -> visitArrowMemberAccess(expression, isRvalue)
        is Conditional       -> visitConditional(expression)
        is CharNode          -> visitCharNode(expression)
        is SingleInitializer -> visitSingleInitializer(expression)
        is InitializerList   -> visitSingleInitializer0(expression.initializers[0] as SingleInitializer)
        is CompoundLiteral   -> visitCompoundLiteral(expression)
        is BuiltinVaStart    -> visitBuiltInVaStart(expression)
        is BuiltinVaArg      -> visitBuiltInVaArg(expression)
        is BuiltinVaEnd      -> visitBuiltInVaEnd(expression)
        else -> throw IRCodeGenError("Unknown expression: $expression")
    }

    private fun visitBuiltInVaEnd(builtinVaEnd: BuiltinVaEnd): Value {
        val vaListType = builtinVaEnd.vaList.resolveType(typeHolder)
        if (vaListType != VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $vaListType")
        }
        // Nothing to do
        return Value.UNDEF
    }

    private fun visitBuiltInVaStart(builtinVaStart: BuiltinVaStart): Value {
        val vaList = visitExpression(builtinVaStart.vaList, true)
        val vaListType = builtinVaStart.vaList.resolveType(typeHolder)
        if (vaListType != VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $vaListType")
        }
        val fnStmt = stmtStack.root()
        val vaInit = fnStmt.vaInit as Alloc
        val cont = ir.createLabel()
        ir.intrinsic(arrayListOf(vaList, vaInit), VaStart(functionType.args()), cont)
        ir.switchLabel(cont)

        return Value.UNDEF
    }

    private fun visitBuiltInVaArg(builtinVaArg: BuiltinVaArg): Value {
        val vaListType = builtinVaArg.assign.resolveType(typeHolder)
        if (vaListType != VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $vaListType")
        }

        val vaList = visitExpression(builtinVaArg.assign, true)
        return when (val argType = builtinVaArg.resolveType(typeHolder)) {
            is CHAR, is UCHAR, is SHORT, is USHORT, is INT, is UINT, is LONG, is ULONG, is CPointer -> {
                val argType = mb.toIRType<PrimitiveType>(typeHolder, argType)
                emitBuiltInVaArg(vaList, argType, VaStart.GP_OFFSET_IDX, VaStart.REG_SAVE_AREA_SIZE)
            }
            is DOUBLE, is FLOAT -> {
                val argType = mb.toIRType<PrimitiveType>(typeHolder, argType)
                emitBuiltInVaArg(vaList, argType, VaStart.FP_OFFSET_IDX, VaStart.FP_REG_SAVE_AREA_SIZE)
            }
            is CStructType -> {
                val irType = mb.toIRType<StructType>(typeHolder, argType)
                val alloc = ir.alloc(irType)
                if (argType.isSmall()) {
                    val types = CallConvention.coerceArgumentTypes(argType) ?: throw IRCodeGenError("Internal error")
                    for ((idx, type) in types.withIndex()) {
                        val fieldPtr = ir.gep(alloc, Type.I8, Constant.valueOf(Type.I64, type.sizeOf() * idx))
                        val arg = when(type) {
                            is PointerType, is IntegerType -> emitBuiltInVaArg(vaList, type, VaStart.GP_OFFSET_IDX, VaStart.REG_SAVE_AREA_SIZE)
                            is FloatingPointType           -> emitBuiltInVaArg(vaList, type, VaStart.FP_OFFSET_IDX, VaStart.FP_REG_SAVE_AREA_SIZE)
                            else -> throw IRCodeGenError("Unknown type $type")
                        }
                        ir.store(fieldPtr, arg)
                    }
                } else {
                    emitBuiltInVarArgLargeStruct(vaList, alloc, irType)
                }
                alloc
            }
            else -> throw IRCodeGenError("Unknown type $argType")
        }
    }

    private fun emitBuiltInVarArgLargeStruct(vaList: Value, dst: Alloc, argType: StructType) {
        val overflowArgAreaPtr = ir.gfp(vaList, vaListIrType, arrayOf(Constant.valueOf(Type.I64, VaStart.OVERFLOW_ARG_AREA_IDX)))
        val argInMem = ir.load(Type.Ptr, overflowArgAreaPtr)
        val inc = ir.gep(argInMem, Type.I8, I32Value(argType.sizeOf()))
        ir.store(overflowArgAreaPtr, inc)

        ir.memcpy(dst, argInMem, U64Value(argType.sizeOf().toLong()))
    }

    private fun emitBuiltInVaArg(vaList: Value, argType: PrimitiveType, offsetIdx: Int, regSaveAreaIdx: Int): Value {
        val gpOffsetPtr = ir.gfp(vaList, vaListIrType, arrayOf(Constant.valueOf(Type.I64, offsetIdx)))
        val gpOffset = ir.load(Type.I32, gpOffsetPtr)

        val varArgInReg = ir.createLabel()
        val varArgInStack = ir.createLabel()
        val cont = ir.createLabel()

        val isReg = ir.icmp(gpOffset, IntPredicate.Le, Constant.valueOf(Type.I32, regSaveAreaIdx))
        ir.branchCond(isReg, varArgInReg, varArgInStack)

        val argInReg = ir.switchLabel(varArgInReg).let {
            val regSaveAreaPtr = ir.gfp(vaList, vaListIrType, arrayOf(Constant.valueOf(Type.I64, VaStart.REG_SAVE_AREA_IDX)))
            val regSaveArea = ir.load(Type.Ptr, regSaveAreaPtr)
            val argInReg = ir.gep(regSaveArea, Type.I8, gpOffset)
            val newGPOffset = ir.add(gpOffset, Constant.valueOf(Type.I32, QWORD_SIZE))
            ir.store(gpOffsetPtr, newGPOffset)
            ir.branch(cont)

            argInReg
        }

        val argInMem = ir.switchLabel(varArgInStack).let {
            val overflowArgAreaPtr = ir.gfp(vaList, vaListIrType, arrayOf(Constant.valueOf(Type.I64, VaStart.OVERFLOW_ARG_AREA_IDX)))
            val argInMem = ir.load(Type.Ptr, overflowArgAreaPtr)
            val inc = ir.gep(argInMem, Type.I64, I32Value(1))
            ir.store(overflowArgAreaPtr, inc)
            ir.branch(cont)

            argInMem
        }

        ir.switchLabel(cont)
        val argPtr = ir.phi(listOf(argInReg, argInMem), listOf(varArgInReg, varArgInStack))
        return ir.load(argType, argPtr)
    }

    private fun visitSingleInitializer0(singleInitializer: SingleInitializer): Value = when (val expr = singleInitializer.expr) {
        is InitializerList -> visitSingleInitializer0(expr.initializers[0] as SingleInitializer)
        else -> visitExpression(expr, true)
    }

    private fun visitSingleInitializer(singleInitializer: SingleInitializer): Value {
        val lvalueAdr = initializerContext.peekValue()
        val type = initializerContext.peekType().cType() as CAggregateType
        val idx = initializerContext.peekIndex()
        when (val expr = singleInitializer.expr) {
            is InitializerList -> when (type) {
                is CArrayType -> {
                    val t = type.element()
                    val irType = mb.toIRType<AggregateType>(typeHolder, t.cType())
                    val fieldPtr = ir.gep(lvalueAdr, irType, Constant.valueOf(Type.I64, idx))
                    initializerContext.scope(fieldPtr, t) { visitInitializerList(expr) }
                }
                is CStructType -> {
                    val t = type.fieldIndex(idx) ?: throw IRCodeGenError("Field not found")
                    val irType = mb.toIRType<AggregateType>(typeHolder, t.cType())
                    val fieldPtr = ir.gfp(lvalueAdr, irType, arrayOf(Constant.valueOf(Type.I64, idx)))
                    initializerContext.scope(fieldPtr, t) { visitInitializerList(expr) }
                }
                else -> throw IRCodeGenError("Unknown type")
            }
            is StringNode -> {
                if (type !is CArrayType) {
                    throw IRCodeGenError("Expect array type, but type=$type")
                }
                when (val type = type.element().cType()) {
                    is CHAR, is UCHAR -> {
                        val string = expr.data()
                        ir.memcpy(lvalueAdr, visitStringNode(expr), U64Value(string.length.toLong()))
                    }
                    is CPointer -> {
                        val string = expr.data()
                        val stringLiteral = StringLiteralGlobalConstant(createStringLiteralName(), ArrayType(Type.I8, string.length), string)
                        val stringPtr = mb.addConstant(stringLiteral)
                        val fieldPtr = ir.gep(lvalueAdr, Type.I64,
                            Constant.valueOf(Type.I64, idx)
                        )
                        ir.store(fieldPtr, stringPtr)
                    }
                    else -> throw IRCodeGenError("Unknown type $type")
                }
            }
            else -> {
                val rvalue = visitExpression(expr, true)
                val irType = mb.toIRType<AggregateType>(typeHolder, type)
                val fieldType = irType.field(idx)
                val converted = ir.convertToType(rvalue, fieldType)
                val fieldPtr = ir.gfp(lvalueAdr, irType, arrayOf(Constant.valueOf(Type.I64, idx)))
                ir.store(fieldPtr, converted)
            }
        }
        return Value.UNDEF
    }

    private fun visitCharNode(charNode: CharNode): Value {
        val charType  = charNode.resolveType(typeHolder)
        val charValue = I8Value(charNode.toInt().toByte())
        return ir.convertToType(charValue, mb.toIRType<PrimitiveType>(typeHolder, charType))
    }

    private fun generateIfElsePattern(commonType: NonTrivialType, conditional: Conditional): Value {
        val condition = makeConditionFromExpression(conditional.cond)

        val trueBB = ir.createLabel()
        val falseBB = ir.createLabel()
        val end = ir.createLabel()
        ir.branchCond(condition, trueBB, falseBB)
        ir.switchLabel(trueBB)

        val right = visitExpression(conditional.eTrue, true)
        val convertedRight = ir.convertToType(right, commonType)

        val trueBBCurrent = ir.currentLabel()
        ir.branch(end)
        ir.switchLabel(falseBB)

        val left = visitExpression(conditional.eFalse, true)
        val convertedLeft = ir.convertToType(left, commonType)

        val falseBBCurrent = ir.currentLabel()
        ir.branch(end)
        ir.switchLabel(end)
        return ir.phi(listOf(convertedRight, convertedLeft), listOf(trueBBCurrent, falseBBCurrent))
    }

    private fun visitConditional(conditional: Conditional): Value {
        when (val commonType = mb.toIRType<Type>(typeHolder, conditional.resolveType(typeHolder))) {
            Type.Void -> {
                val condition = makeConditionFromExpression(conditional.cond)
                val thenBlock = ir.createLabel()
                val elseBlock = ir.createLabel()
                val exitBlock = ir.createLabel()

                ir.branchCond(condition, thenBlock, elseBlock)
                ir.switchLabel(thenBlock)
                visitExpression(conditional.eTrue, true)
                ir.branch(exitBlock)

                ir.switchLabel(elseBlock)
                visitExpression(conditional.eFalse, true)
                ir.branch(exitBlock)

                ir.switchLabel(exitBlock)
                return Value.UNDEF
            }
            is IntegerType -> { // TODO: pointer type also can be here
                val onTrue = constEvalExpression0(conditional.eTrue) ?:
                    return generateIfElsePattern(commonType, conditional)

                val onFalse = constEvalExpression0(conditional.eFalse) ?:
                    return generateIfElsePattern(commonType, conditional)

                val onTrueContant  = NonTrivialConstant.of(commonType, onTrue)
                val onFalseContant = NonTrivialConstant.of(commonType, onFalse)
                val condition = makeConditionFromExpression(conditional.cond)
                return ir.select(condition, commonType, onTrueContant, onFalseContant)
            }
            is NonTrivialType -> return generateIfElsePattern(commonType, conditional)
            else -> throw IRCodeGenError("Unknown type: $commonType")
        }
    }

    private fun visitArrowMemberAccess(arrowMemberAccess: ArrowMemberAccess, isRvalue: Boolean): Value {
        val struct   = visitExpression(arrowMemberAccess.primary, true)
        val cPointer = when (val ty = arrowMemberAccess.primary.resolveType(typeHolder)) {
            is AnyCArrayType -> ty.asPointer()
            is CPointer      -> ty
            else -> throw IRCodeGenError("Pointer type expected, but got $ty")
        }
        val cStructType = cPointer.dereference(typeHolder)
        val structIRType = mb.toIRType<StructType>(typeHolder, cStructType)

        if (cStructType !is AnyCStructType) {
            throw IRCodeGenError("Struct type expected, but got '$cStructType'")
        }
        val fieldName = arrowMemberAccess.fieldName()
        val member = cStructType.fieldIndex(fieldName) ?: let {
            throw IRCodeGenError("Field not found: $fieldName")
        }

        val indexes = arrayOf(Constant.valueOf<IntegerConstant>(Type.I64, member))
        val gep = ir.gfp(struct, structIRType, indexes)
        if (!isRvalue) {
            return gep
        }

        val memberType = cStructType.field(fieldName) ?: throw IRCodeGenError("Field not found: $fieldName")
        if (memberType is CAggregateType) {
            return gep
        }
        val memberIRType = mb.toIRLVType<PrimitiveType>(typeHolder, memberType)
        return ir.load(memberIRType, gep)
    }

    private fun visitMemberAccess(memberAccess: MemberAccess, isRvalue: Boolean): Value {
        val struct = visitExpression(memberAccess.primary, false) //TODO isRvalue???
        val structType = memberAccess.primary.resolveType(typeHolder)
        if (structType !is AnyCStructType) {
            throw IRCodeGenError("Struct type expected, but got '$structType'")
        }
        val structIRType = mb.toIRType<StructType>(typeHolder, structType)

        val fieldName = memberAccess.memberName()
        val member = structType.fieldIndex(fieldName) ?: throw IRCodeGenError("Field not found: $fieldName")

        val indexes = arrayOf(Constant.valueOf<IntegerConstant>(Type.I64, member))
        val gep = ir.gfp(struct, structIRType, indexes)
        if (!isRvalue) {
            return gep
        }
        val memberType = structType.field(fieldName) ?: throw IRCodeGenError("Field not found: $fieldName")
        if (memberType is CAggregateType) {
            return gep
        }

        val memberIRType = mb.toIRLVType<PrimitiveType>(typeHolder, memberType)
        return ir.load(memberIRType, gep)
    }

    private fun visitSizeOf(sizeOf: SizeOf): Value = when (val expr = sizeOf.expr) {
        is TypeName -> {
            val resolved = expr.specifyType(typeHolder, listOf())
            val irType = mb.toIRType<NonTrivialType>(typeHolder, resolved.type.cType())
            I64Value(irType.sizeOf().toLong())
        }
        is Expression -> {
            val resolved = expr.resolveType(typeHolder)
            val irType = mb.toIRType<NonTrivialType>(typeHolder, resolved)
            I64Value(irType.sizeOf().toLong())
        }
        else -> throw IRCodeGenError("Unknown sizeOf expression, expr=${expr}")
    }

    private fun visitStringNode(stringNode: StringNode): Value {
        val string = stringNode.data()
        val stringLiteral = StringLiteralGlobalConstant(createStringLiteralName(), ArrayType(Type.I8, string.length), string)
        return mb.addConstant(stringLiteral)
    }

    private fun visitArrayAccess(arrayAccess: ArrayAccess, isRvalue: Boolean): Value {
        val index = visitExpression(arrayAccess.expr, true)
        val convertedIndex = ir.toIndexType(index)
        val array = visitExpression(arrayAccess.primary, true)

        val arrayType = arrayAccess.resolveType(typeHolder)
        val elementType = mb.toIRType<NonTrivialType>(typeHolder, arrayType)

        val adr = ir.gep(array, elementType, convertedIndex)
        if (!isRvalue) {
            return adr
        }
        if (arrayType is CAggregateType) {
            return adr
        }
        return ir.load(elementType as PrimitiveType, adr)
    }

    private fun convertArg(function: AnyFunctionPrototype, argIdx: Int, expr: Value): Value {
        if (argIdx < function.arguments().size) {
            val cvt = function.argument(argIdx)
            return ir.convertToType(expr, cvt)
        }

        if (!function.attributes.contains(VarArgAttribute)) {
            throw IRCodeGenError("Too many arguments in function call '${function.shortDescription()}'")
        }
        return when (expr.type()) {
            Type.F32          -> ir.convertToType(expr, Type.F64)
            Type.I8, Type.I16 -> ir.convertToType(expr, Type.I32)
            Type.U8, Type.U16 -> ir.convertToType(expr, Type.U32)
            Type.U1           -> ir.convertToType(expr, Type.I32)
            else -> expr
        }
    }

    private fun convertFunctionArgs(function: AnyFunctionPrototype, args: List<Expression>): Pair<List<Value>, Set<FunctionAttribute>> {
        val convertedArgs = mutableListOf<Value>()
        val attributes = hashSetOf<FunctionAttribute>()
        var offset = 0
        for ((idx, argValue) in args.withIndex()) {
            val expr = visitExpression(argValue, true)
            when (val argCType = argValue.resolveType(typeHolder)) {
                is CPrimitive, is CFunctionType, is CUncompletedArrayType, is CStringLiteral -> {
                    val convertedArg = convertArg(function, idx + offset, expr)
                    convertedArgs.add(convertedArg)
                }
                is CArrayType -> {
                    val type = mb.toIRType<ArrayType>(typeHolder, argCType)
                    val convertedArg = ir.gep(expr, type.elementType(), I64Value(0))
                    convertedArgs.add(convertedArg)
                }
                is AnyCStructType -> {
                    if (!argCType.isSmall()) {
                        attributes.add(ByValue(idx))
                    }
                    val argValues = ir.coerceArguments(argCType, expr)
                    convertedArgs.addAll(argValues)
                    offset += argValues.size - 1
                }
                else -> throw IRCodeGenError("Unknown type, type=${argCType} in function call")
            }
        }
        return Pair(convertedArgs, attributes)
    }

    private fun visitCast(cast: Cast): Value {
        val value = visitExpression(cast.cast, true)
        val castToType = cast.resolveType(typeHolder)
        return when (val toType = mb.toIRType<Type>(typeHolder, castToType)) {
            Type.Void -> value
            Type.U1   -> ir.convertToType(value, Type.U1)
            Type.Ptr  -> {
                val baseAddr = when (val fromType = cast.cast.resolveType(typeHolder)) {
                    is CArrayType -> {
                        val irType = mb.toIRType<ArrayType>(typeHolder, fromType)
                        ir.gep(value, irType.elementType(), I64Value(0))
                    }
                    is CStructType -> {
                        val irType = mb.toIRType<StructType>(typeHolder, fromType)
                        ir.gep(value, irType, I64Value(0))
                    }
                    is CPrimitive, is CFunctionType, is CStringLiteral -> value
                    else -> throw IRCodeGenError("Cannon cast to pointer from type $fromType")
                }
                ir.convertToType(baseAddr, Type.Ptr)
            }
            else -> ir.convertToType(value, toType)
        }
    }

    private fun visitFunPointerCall(funcPointerCall: FunctionCall): Value {
        val functionType = funcPointerCall.functionType(typeHolder)
        val loadedFunctionPtr = visitExpression(funcPointerCall.primary, true)

        val irRetType = mb.toIRType<Type>(typeHolder, functionType.retType().cType())
        val argTypes = functionType.args().map {
            mb.toIRType<NonTrivialType>(typeHolder, it.cType())
        }
        val prototypeAttributes = if (functionType.variadic()) {
            hashSetOf(VarArgAttribute)
        } else {
            emptySet()
        }
        val prototype = IndirectFunctionPrototype(irRetType, argTypes, prototypeAttributes)
        val (convertedArgs, attr) = convertFunctionArgs(prototype, funcPointerCall.args)

        val attributes = prototypeAttributes + attr

        val cont = ir.createLabel()
        val ret = when (functionType.retType().cType()) {
            VOID -> {
                ir.ivcall(loadedFunctionPtr, prototype, convertedArgs, attributes, cont)
                Value.UNDEF
            }
            is CPrimitive -> ir.icall(loadedFunctionPtr, prototype, convertedArgs, attributes, cont)
            is CStructType -> when (prototype.returnType()) {
                is PrimitiveType -> ir.icall(loadedFunctionPtr, prototype, convertedArgs, attributes, cont)
                //is TupleType     -> ir.tupleCall(function, convertedArgs, cont)
                is StructType    -> ir.icall(loadedFunctionPtr, prototype, convertedArgs, attributes, cont)
                else -> throw IRCodeGenError("Unknown type ${functionType.retType()}")
            }
            else -> throw IRCodeGenError("Unknown type ${functionType.retType()}")
        }
        ir.switchLabel(cont)
        return ret
    }

    private fun visitFunctionCall(functionCall: FunctionCall): Value {
        val primary = functionCall.primary
        if (primary !is VarNode) {
            return visitFunPointerCall(functionCall)
        }
        val name = primary.name()
        val function = mb.findFunction(name) ?: return visitFunPointerCall(functionCall)
        val (convertedArgs, attributes) = convertFunctionArgs(function, functionCall.args)
        val cont = ir.createLabel()
        return when (val functionType = functionCall.resolveType(typeHolder)) {
            VOID -> {
                ir.vcall(function, convertedArgs, attributes, cont)
                ir.switchLabel(cont)
                Value.UNDEF
            }
            is CPrimitive -> {
                val call = ir.call(function, convertedArgs, attributes, cont)
                ir.switchLabel(cont)
                call
            }
            is CStructType -> when (val t = function.returnType()) {
                is PrimitiveType -> {
                    val alloc = ir.alloc(t)
                    val call = ir.call(function, convertedArgs, attributes, cont)
                    ir.switchLabel(cont)
                    ir.store(alloc, call)
                    alloc
                }
                is TupleType     -> {
                    val tup = ir.tupleCall(function, convertedArgs, attributes, cont)
                    ir.switchLabel(cont)
                    tup
                }
                is VoidType      -> {
                    val retType = mb.toIRType<StructType>(typeHolder, functionType)
                    val retValue = ir.alloc(retType)
                    ir.vcall(function, arrayListOf(retValue) + convertedArgs, attributes, cont)
                    ir.switchLabel(cont)
                    retValue
                }
                else -> throw IRCodeGenError("Unknown type ${function.returnType()}")
            }

            else -> TODO("$functionType")
        }
    }

    private fun eq(type: Type): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Eq
        is FloatingPointType -> FloatPredicate.Oeq
        is PointerType       -> IntPredicate.Eq
        else -> throw IRCodeGenError("Unknown type")
    }

    private fun ne(type: Type): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Ne
        is FloatingPointType -> FloatPredicate.One
        is PointerType       -> IntPredicate.Ne
        else -> throw IRCodeGenError("Unknown type")
    }

    private fun gt(type: Type): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Gt
        is FloatingPointType -> FloatPredicate.Ogt
        is PointerType       -> IntPredicate.Gt
        else -> throw IRCodeGenError("Unknown type")
    }

    private fun lt(type: Type): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Lt
        is FloatingPointType -> FloatPredicate.Olt
        is PointerType       -> IntPredicate.Lt
        else -> throw IRCodeGenError("Unknown type")
    }

    private fun le(type: Type): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Le
        is FloatingPointType -> FloatPredicate.Ole
        is PointerType       -> IntPredicate.Le
        else -> throw IRCodeGenError("Unknown type")
    }

    private fun ge(type: Type): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Ge
        is FloatingPointType -> FloatPredicate.Oge
        is PointerType       -> IntPredicate.Ge
        else -> throw IRCodeGenError("Unknown type")
    }

    private fun makeAlgebraicBinary(binop: BinaryOp, op: (a: Value, b: Value) -> Value): Value {
        when (val commonType = mb.toIRLVType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))) {
            is PointerType -> {
                val lvalue     = visitExpression(binop.left, true)
                val lValueType = when (val l = binop.left.resolveType(typeHolder)) {
                    is AnyCArrayType -> l.asPointer()
                    is CPointer      -> l
                    else -> throw IRCodeGenError("Pointer type expected, but got $l")
                }
                val convertedLValue = ir.convertToType(lvalue, Type.U64)

                val rvalue = visitExpression(binop.right, true)
                when (val r = binop.right.resolveType(typeHolder)) {
                    is AnyCArrayType, is CPrimitive -> {}
                    else -> throw IRCodeGenError("Primitive type expected, but got $r")
                }
                val convertedRValue = ir.convertToType(rvalue, Type.U64)

                val size = lValueType.dereference(typeHolder).size()
                val mul = ir.mul(convertedRValue, U64Value(size.toLong()))

                val result = op(convertedLValue, mul)
                return ir.convertToType(result, commonType)
            }
            is FloatingPointType -> {
                val left = visitExpression(binop.left, true)
                val leftConverted = ir.convertToType(left, commonType)

                val right = visitExpression(binop.right, true)
                val rightConverted = ir.convertToType(right, commonType)

                return op(leftConverted, rightConverted)
            }
            is IntegerType -> {
                val cvtType = when (commonType) {
                    is SignedIntType -> if (commonType.sizeOf() < WORD_SIZE) Type.I32 else commonType
                    is UnsignedIntType -> if (commonType.sizeOf() < WORD_SIZE) Type.U32 else commonType
                }
                val left = visitExpression(binop.left, true)
                val leftConverted = ir.convertToType(left, cvtType)

                val right = visitExpression(binop.right, true)
                val rightConverted = ir.convertToType(right, cvtType)

                return op(leftConverted, rightConverted)
            }
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun makeAlgebraicBinaryWithAssignment(binop: BinaryOp, op: (a: Value, b: Value) -> Value): Value {
        when (val commonType = mb.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))) {
            is PointerType -> {
                val rvalue     = visitExpression(binop.right, true)
                val rValueType = binop.right.resolveType(typeHolder)
                if (rValueType !is CPrimitive) {
                    throw IRCodeGenError("Primitive type expected")
                }
                val convertedRValue = ir.convertToType(rvalue, Type.U64)

                val lvalueAddress = visitExpression(binop.left, false)
                val lValueType    = binop.left.resolveType(typeHolder)
                val lvalue        = ir.load(Type.Ptr, lvalueAddress)
                val ptr2intLValue = ir.ptr2int(lvalue, Type.U64)

                if (lValueType !is CPointer) {
                    throw IRCodeGenError("Pointer type expected")
                }
                val convertedLValue = ir.convertToType(ptr2intLValue, Type.U64)

                val size = lValueType.dereference(typeHolder).size()
                val mul = ir.mul(convertedRValue, U64Value(size.toLong()))

                val result = op(convertedLValue, mul)
                val res = ir.convertToType(result, commonType)
                ir.store(lvalueAddress, res)
                return res
            }
            is FloatingPointType -> {
                val right = visitExpression(binop.right, true)
                val leftType = binop.left.resolveType(typeHolder)
                val leftIrType = mb.toIRType<PrimitiveType>(typeHolder, leftType)
                val rightConverted = ir.convertToType(right, leftIrType)

                val left = visitExpression(binop.left, false)
                val loadedLeft = ir.load(leftIrType, left)

                val sum = op(loadedLeft, rightConverted)
                ir.store(left, sum)
                return sum
            }
            is IntegerType -> {
                val right = visitExpression(binop.right, true)
                val leftType = binop.left.resolveType(typeHolder)
                val originalIrType = mb.toIRType<IntegerType>(typeHolder, leftType)
                val leftIrType = when (originalIrType) {
                    is SignedIntType -> if (originalIrType.sizeOf() < WORD_SIZE) Type.I32 else originalIrType
                    is UnsignedIntType -> if (originalIrType.sizeOf() < WORD_SIZE) Type.U32 else originalIrType
                }
                val rightConverted = ir.convertToType(right, leftIrType)

                val left = visitExpression(binop.left, false)
                val loadedLeft = ir.load(originalIrType, left)
                val cvtLft = ir.convertToType(loadedLeft, leftIrType)

                val sum = op(cvtLft, rightConverted)
                val sumCvt = ir.convertToType(sum, originalIrType)
                ir.store(left, sumCvt)
                return sum
            }
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private inline fun makeComparisonBinary(binop: BinaryOp, crossinline predicate: (NonTrivialType) -> AnyPredicateType, isRvalue: Boolean): Value {
        val commonType = mb.toIRLVType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))
        val left = visitExpression(binop.left, true)
        val leftConverted = ir.convertToType(left, commonType)

        val right = visitExpression(binop.right, true)
        val rightConverted = ir.convertToType(right, commonType)

        val cmp = makeCondition(leftConverted, predicate(commonType), rightConverted)
        if (isRvalue) {
            return cmp
        }
        return ir.convertToType(cmp, Type.I8)
    }

    private fun visitAssignBinary(binop: BinaryOp): Value {
        val right = visitExpression(binop.right, true)
        val leftType = binop.left.resolveType(typeHolder)

        if (leftType !is AnyCStructType) {
            val leftIrType = mb.toIRLVType<PrimitiveType>(typeHolder, leftType)
            val rightCvt = ir.convertToType(right, leftIrType)

            val left = visitExpression(binop.left, false)
            ir.store(left, rightCvt)
            return rightCvt //TODO test it
        }

        val left = visitExpression(binop.left, true)
        if (!leftType.isSmall()) {
            ir.memcpy(left, right, U64Value(leftType.size().toLong()))
            return right
        }
        val list = CallConvention.coerceArgumentTypes(leftType) ?: throw IRCodeGenError("Internal error")
        val structType = mb.toIRType<AggregateType>(typeHolder, leftType)
        if (list.size == 1) {
            val loadedRight = ir.load(list[0], right)
            val gep = ir.gep(left, structType, I64Value(0))
            ir.store(gep, loadedRight)
        } else {
            list.forEachIndexed { idx, arg ->
                val offset   = (idx * QWORD_SIZE) / arg.sizeOf()
                val fieldPtr = ir.gep(left, arg, Constant.valueOf(Type.I64, offset))
                val proj = ir.proj(right, idx)
                ir.store(fieldPtr, proj)
            }
        }
        return right
    }

    private fun visitBinary(binop: BinaryOp, isRvalue: Boolean): Value = when (binop.opType) {
        BinaryOpType.ADD -> makeAlgebraicBinary(binop, ir::add)
        BinaryOpType.SUB -> makeAlgebraicBinary(binop, ir::sub)
        BinaryOpType.ASSIGN -> visitAssignBinary(binop)
        BinaryOpType.ADD_ASSIGN -> makeAlgebraicBinaryWithAssignment(binop, ir::add)
        BinaryOpType.DIV_ASSIGN -> makeAlgebraicBinaryWithAssignment(binop, ::divide)
        BinaryOpType.MUL_ASSIGN -> makeAlgebraicBinaryWithAssignment(binop, ir::mul)
        BinaryOpType.BIT_OR -> makeAlgebraicBinary(binop, ir::or)
        BinaryOpType.MUL -> makeAlgebraicBinary(binop, ir::mul)
        BinaryOpType.NE -> makeComparisonBinary(binop, ::ne, isRvalue)
        BinaryOpType.GT -> makeComparisonBinary(binop, ::gt, isRvalue)
        BinaryOpType.LT -> makeComparisonBinary(binop, ::lt, isRvalue)
        BinaryOpType.LE -> makeComparisonBinary(binop, ::le, isRvalue)
        BinaryOpType.AND -> {
            val left = visitExpression(binop.left, true)
            val convertedLeft = ir.convertToType(left, Type.U1)

            val bb = ir.createLabel()
            val end = ir.createLabel()
            val initialBB = ir.currentLabel()
            ir.branchCond(convertedLeft, bb, end)
            ir.switchLabel(bb)

            val right = visitExpression(binop.right, true)
            val convertedRight = ir.convertToType(right, Type.U8) //TODO I8???

            val current = ir.currentLabel()
            ir.branch(end)
            ir.switchLabel(end)
            ir.phi(listOf(U8Value(0), convertedRight), listOf(initialBB, current))
        }
        BinaryOpType.OR -> {
            val left = visitExpression(binop.left, true)
            val convertedLeft = ir.convertToType(left, Type.U1)

            val initialBB = ir.currentLabel()
            val bb = ir.createLabel()
            val end = ir.createLabel()
            ir.branchCond(convertedLeft, end, bb)
            ir.switchLabel(bb)

            val right = visitExpression(binop.right, true)
            val convertedRight = ir.convertToType(right, Type.U8) //TODO I8???

            val current = ir.currentLabel()
            ir.branch(end)
            ir.switchLabel(end)
            ir.phi(listOf(U8Value(1), convertedRight), listOf(initialBB, current))
        }
        BinaryOpType.SHR_ASSIGN -> makeAlgebraicBinaryWithAssignment(binop, ir::shr)
        BinaryOpType.SHL_ASSIGN -> makeAlgebraicBinaryWithAssignment(binop, ir::shl)
        BinaryOpType.BIT_XOR_ASSIGN -> makeAlgebraicBinaryWithAssignment(binop, ir::xor)
        BinaryOpType.BIT_OR_ASSIGN -> makeAlgebraicBinaryWithAssignment(binop, ir::or)
        BinaryOpType.GE  -> makeComparisonBinary(binop, ::ge, isRvalue)
        BinaryOpType.EQ  -> makeComparisonBinary(binop, ::eq, isRvalue)
        BinaryOpType.SHL -> makeAlgebraicBinary(binop, ir::shl)
        BinaryOpType.SHR -> makeAlgebraicBinary(binop, ir::shr)
        BinaryOpType.BIT_AND -> makeAlgebraicBinary(binop, ir::and)
        BinaryOpType.BIT_XOR -> makeAlgebraicBinary(binop, ir::xor)
        BinaryOpType.MOD -> makeAlgebraicBinary(binop, ::rem)
        BinaryOpType.DIV -> makeAlgebraicBinary(binop, ::divide)
        BinaryOpType.SUB_ASSIGN -> makeAlgebraicBinaryWithAssignment(binop, ir::sub)
        BinaryOpType.MOD_ASSIGN -> makeAlgebraicBinaryWithAssignment(binop, ::rem)
        BinaryOpType.BIT_AND_ASSIGN -> makeAlgebraicBinaryWithAssignment(binop, ir::and)
        BinaryOpType.COMMA -> {
            visitExpression(binop.left, false)
            visitExpression(binop.right, false)
        }
    }

    private fun divide(a: Value, b: Value): Value = when (a.type()) {
        is IntegerType -> {
            val tupleDiv = ir.tupleDiv(a, b)
            ir.proj(tupleDiv, 0)
        }
        is FloatingPointType -> ir.div(a, b)
        else -> throw IRCodeGenError("Unknown type")
    }

    private fun rem(a: Value, b: Value): Value = when (a.type()) {
        is IntegerType -> {
            val tupleDiv = ir.tupleDiv(a, b)
            ir.proj(tupleDiv, 1)
        }
        else -> throw IRCodeGenError("Unknown type")
    }

    private fun visitIncOrDec(unaryOp: UnaryOp, op: (a: Value, b: Value) -> LocalValue): Value {
        assertion(unaryOp.opType == PostfixUnaryOpType.INC || unaryOp.opType == PostfixUnaryOpType.DEC) {
            "Unknown operation, op=${unaryOp.opType}"
        }

        val ctype = unaryOp.resolveType(typeHolder)
        val addr = visitExpression(unaryOp.primary, false)
        val type = mb.toIRType<PrimitiveType>(typeHolder, ctype)
        val loaded = ir.load(type, addr)
        when (ctype) {
            is CPointer -> {
                val converted = ir.convertToType(loaded, Type.I64)
                val inc = op(converted, Constant.of(Type.I64, ctype.dereference(typeHolder).size()))
                ir.store(addr, ir.convertToType(inc, type))
            }
            is CPrimitive -> {
                val inc = op(loaded, Constant.of(loaded.type(), 1))
                ir.store(addr, ir.convertToType(inc, type))
            }
            else -> throw IRCodeGenError("Unknown type")
        }
        return loaded
    }

    private fun visitPrefixIncOrDec(unaryOp: UnaryOp, op: (a: Value, b: Value) -> LocalValue): Value {
        assertion(unaryOp.opType == PrefixUnaryOpType.INC || unaryOp.opType == PrefixUnaryOpType.DEC) {
            "Unknown operation, op=${unaryOp.opType}"
        }

        val ctype = unaryOp.resolveType(typeHolder)

        val addr = visitExpression(unaryOp.primary, false)
        val type = mb.toIRType<PrimitiveType>(typeHolder, ctype)
        val loaded = ir.load(type, addr)

        when (ctype) {
            is CPointer -> {
                val converted = ir.convertToType(loaded, Type.I64)
                val inc = op(converted, Constant.of(Type.I64, ctype.dereference(typeHolder).size()))
                ir.store(addr, ir.convertToType(inc, type))
                return inc
            }
            is CPrimitive -> {
                val inc = op(loaded, Constant.of(loaded.type(), 1))
                ir.store(addr, ir.convertToType(inc, type))
                return inc
            }
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun visitUnary(unaryOp: UnaryOp, isRvalue: Boolean): Value {
        return when (unaryOp.opType) {
            PrefixUnaryOpType.ADDRESS -> visitExpression(unaryOp.primary, false)
            PrefixUnaryOpType.DEREF -> {
                val addr = visitExpression(unaryOp.primary, true)
                if (!isRvalue) {
                    return addr
                }
                val type = unaryOp.resolveType(typeHolder)
                if (type is CAggregateType) {
                    return addr
                }
                val loadedType = mb.toIRType<PrimitiveType>(typeHolder, type)
                ir.load(loadedType, addr)
            }
            PostfixUnaryOpType.INC -> visitIncOrDec(unaryOp, ir::add)
            PostfixUnaryOpType.DEC -> visitIncOrDec(unaryOp, ir::sub)
            PrefixUnaryOpType.INC  -> visitPrefixIncOrDec(unaryOp, ir::add)
            PrefixUnaryOpType.DEC  -> visitPrefixIncOrDec(unaryOp, ir::sub)
            PrefixUnaryOpType.NEG  -> {
                val value = visitExpression(unaryOp.primary, true)
                val type = unaryOp.resolveType(typeHolder)
                val valueType = mb.toIRType<NonTrivialType>(typeHolder, type)
                val converted = ir.convertToType(value, valueType)
                ir.neg(converted)
            }
            PrefixUnaryOpType.NOT -> {
                val value = visitExpression(unaryOp.primary, true)
                val type = unaryOp.resolveType(typeHolder)
                val commonType = mb.toIRLVType<PrimitiveType>(typeHolder, type)
                val converted = ir.convertToType(value, commonType) //TODO do we need this?
                makeCondition(converted, eq(commonType), Constant.of(converted.asType(), 0))
            }
            PrefixUnaryOpType.BIT_NOT -> {
                val value = visitExpression(unaryOp.primary, true)
                val type = unaryOp.resolveType(typeHolder)
                val commonType = mb.toIRType<NonTrivialType>(typeHolder, type)
                val converted = ir.convertToType(value, commonType)
                ir.not(converted)
            }
            PrefixUnaryOpType.PLUS -> visitExpression(unaryOp.primary, isRvalue)
        }
    }

    private fun visitNumNode(numNode: NumNode): Constant = makeConstant(numNode)

    private fun getVariableAddress(varNode: VarNode, rvalueAddr: Value, isRvalue: Boolean): Value {
        if (!isRvalue) {
            return rvalueAddr
        }
        val type = varNode.resolveType(typeHolder)
        if (type is CAggregateType) {
            return rvalueAddr
        }
        val converted = mb.toIRLVType<PrimitiveType>(typeHolder, type)
        return ir.load(converted, rvalueAddr)
    }

    private fun visitVarNode(varNode: VarNode, isRvalue: Boolean): Value {
        val name = varNode.name()
        val rvalueAttr = varStack[name]
        if (rvalueAttr != null) {
            return getVariableAddress(varNode, rvalueAttr, isRvalue)
        }
        val global = mb.findFunction(name)
        if (global != null) {
            return global
        }

        val enumValue = typeHolder.findEnumByEnumerator(name)
        if (enumValue != null) {
            return Constant.of(Type.I32, enumValue)
        }

        throw IRCodeGenError("Variable '$name' not found: ${varNode.position()}")
    }

    private fun visitParameters(parameters: List<String>,
                                cTypes: List<TypeDesc>,
                                arguments: List<ArgumentValue>,
                                closure: (String, CType, List<ArgumentValue>) -> Unit) {
        var argumentIdx = 0
        for (currentArg in cTypes.indices) {
            when (val cType = cTypes[currentArg].cType()) {
                is CPrimitive -> {
                    closure(parameters[currentArg], cType, listOf(arguments[argumentIdx]))
                }
                is AnyCStructType -> {
                    val types = CallConvention.coerceArgumentTypes(cType) ?: listOf(Type.Ptr)
                    val args = mutableListOf<ArgumentValue>()
                    for (i in types.indices) {
                        args.add(arguments[argumentIdx + i])
                    }
                    argumentIdx += types.size - 1
                    closure(parameters[currentArg], cType, args)
                }
                is AnyCArrayType -> {
                    closure(parameters[currentArg], cType, listOf(arguments[argumentIdx]))
                }
                else -> throw IRCodeGenError("Unknown type, type=$cType")
            }
            argumentIdx++
        }
    }

    private fun visitParameter(param: String, cType: CType, args: List<ArgumentValue>) = when (cType) {
        is AnyCArrayType -> {
            assertion(args.size == 1) { "invariant" }
            varStack[param] = args[0]
        }
        is CPrimitive -> {
            assertion(args.size == 1) { "invariant" }

            val irType    = mb.toIRLVType<PrimitiveType>(typeHolder, cType)
            val rvalueAdr = ir.alloc(irType)
            ir.store(rvalueAdr, ir.convertToType(args[0], irType))
            varStack[param] = rvalueAdr
        }
        is AnyCStructType -> {
            if (cType.isSmall()) {
                val irType    = mb.toIRType<NonTrivialType>(typeHolder, cType)
                val rvalueAdr = ir.alloc(irType)
                args.forEachIndexed { idx, arg ->
                    val offset   = (idx * QWORD_SIZE) / arg.type().sizeOf()
                    val fieldPtr = ir.gep(rvalueAdr, arg.type(),
                        Constant.valueOf(Type.I64, offset)
                    )
                    ir.store(fieldPtr, arg)
                }
                varStack[param] = rvalueAdr
            } else {
                assertion(args.size == 1) { "invariant" }
                varStack[param] = args[0]
            }
        }
        else -> throw IRCodeGenError("Unknown type, type=$cType")
    }

    private fun emitReturnType(fnStmt: FunctionStmtInfo, retCType: TypeDesc, args: List<ArgumentValue>) {
        val exitBlock = fnStmt.resolveExit(ir)
        when (val cType = retCType.cType()) {
            is VOID -> {
                ir.switchLabel(exitBlock)
                ir.retVoid()
            }
            is BOOL -> {
                val returnValueAdr = fnStmt.resolveReturnValueAdr(ir.alloc(Type.I8))
                ir.switchLabel(exitBlock)
                val ret = ir.load(Type.I8, returnValueAdr)
                ir.ret(Type.I8, arrayOf(ret))
            }
            is CPrimitive -> {
                val retType = mb.toIRLVType<PrimitiveType>(typeHolder, retCType.cType())
                val returnValueAdr = fnStmt.resolveReturnValueAdr(ir.alloc(retType))
                ir.switchLabel(exitBlock)
                val ret = ir.load(retType, returnValueAdr)
                ir.ret(retType, arrayOf(ret))
            }
            is AnyCStructType -> {
                if (!cType.isSmall()) {
                    fnStmt.resolveReturnValueAdr(args[0])
                    ir.switchLabel(exitBlock)
                    ir.retVoid()
                    return
                }
                val alloc = ir.alloc(mb.toIRType<StructType>(typeHolder, cType))
                val returnValueAdr = fnStmt.resolveReturnValueAdr(alloc)
                ir.switchLabel(exitBlock)
                val retTupleType = CallConvention.coerceArgumentTypes(cType) ?: throw IRCodeGenError("Unknown type, type=$cType")
                val retValues = ir.coerceArguments(cType, returnValueAdr)
                if (retTupleType.size == 1) {
                    ir.ret(retTupleType[0], retValues.toTypedArray())
                } else {
                    ir.ret(TupleType(retTupleType.toTypedArray()), retValues.toTypedArray())
                }
            }
            else -> throw IRCodeGenError("Unknown return type, type=$cType")
        }
    }

    private fun initializeVarArgs(fnType: CFunctionType, fnStmt: FunctionStmtInfo) {
        if (!fnType.variadic()) {
            return
        }
        val vaInitType = mb.toIRType<StructType>(typeHolder, VaInit.vaInit)
        val vaInitInstance = ir.alloc(vaInitType)
        fnStmt.vaInit = vaInitInstance
        val cont = ir.createLabel()
        ir.intrinsic(arrayListOf(vaInitInstance), VaInit(fnType.args().first().cType()), cont)
        ir.switchLabel(cont)
    }

    override fun visit(functionNode: FunctionNode): Value {
        TODO()
    }

    fun visitFun(parameters: List<String>, functionNode: FunctionNode): Value = scoped {
        stmtStack.scoped(FunctionStmtInfo()) { stmt ->
            visitParameters(parameters, functionType.args(), ir.arguments()) { param, cType, args ->
                visitParameter(param, cType, args)
            }

            emitReturnType(stmt, functionType.retType(), ir.arguments())

            ir.switchLabel(Label.entry)
            initializeVarArgs(functionType, stmt)
            visitStatement(functionNode.body)

            if (ir.last() !is TerminateInstruction) {
                ir.branch(stmt.resolveExit(ir))
            }
        }
        return@scoped ir.prototype()
    }

    private fun visitStatement(statement: Statement) = scoped {
        statement.accept(this)
    }

    override fun visit(emptyStatement: EmptyStatement) {}

    override fun visit(exprStatement: ExprStatement) {
        if (ir.last() is TerminateInstruction) {
            return
        }
        visitExpression(exprStatement.expr, true)
    }

    override fun visit(labeledStatement: LabeledStatement) {
        if (ir.last() is TerminateInstruction && labeledStatement.gotos().isEmpty()) {
            return
        }
        val label = seekOrAddLabel(labeledStatement.name())
        if (ir.last() !is TerminateInstruction) {
            ir.branch(label)
        }
        ir.switchLabel(label)
        visitStatement(labeledStatement.stmt)
    }

    override fun visit(gotoStatement: GotoStatement) {
        if (ir.last() is TerminateInstruction) {
            return
        }
        if (gotoStatement.label() == null) {
            throw IRCodeGenError("Goto statement outside of labeled statement")
        }

        val label = seekOrAddLabel(gotoStatement.id.str())
        ir.branch(label)
    }

    override fun visit(continueStatement: ContinueStatement) {
        if (ir.last() is TerminateInstruction) {
            return
        }

        when (val loopInfo = stmtStack.topLoop()) {
            is ForLoopStmtInfo -> ir.branch(loopInfo.resolveUpdate(ir))
            is LoopStmtInfo -> ir.branch(loopInfo.resolveCondition(ir))
            else -> throw IRCodeGenError("Continue statement outside of loop")
        }
    }

    override fun visit(breakStatement: BreakStatement) {
        if (ir.last() is TerminateInstruction) {
            return
        }

        val loopInfo = stmtStack.topSwitchOrLoop() ?: throw IRCodeGenError("Break statement outside of loop or switch")
        ir.branch(loopInfo.resolveExit(ir))
    }

    override fun visit(defaultStatement: DefaultStatement) = scoped {
        val switchInfo = stmtStack.top() as SwitchStmtInfo
        ir.switchLabel(switchInfo.resolveDefault(ir))
        visitStatement(defaultStatement.stmt)
    }

    override fun visit(caseStatement: CaseStatement) = scoped {
        val switchInfo = stmtStack.top() as SwitchStmtInfo

        val ctx = CommonConstEvalContext<Int>(typeHolder)
        val constant = ConstEvalExpression.eval(caseStatement.constExpression, TryConstEvalExpressionInt(ctx))
        if (constant == null) {
            throw IRCodeGenError("Case statement with non-constant expression: ${LineAgnosticAstPrinter.print(caseStatement.constExpression)}")
        }

        val caseValueConverted = Constant.of(switchInfo.conditionType.asType(), constant)
        val caseBlock = ir.createLabel()
        if (switchInfo.table.isNotEmpty() && ir.last() !is TerminateInstruction) {
            // fall through
            ir.branch(caseBlock)
        }

        switchInfo.table.add(caseBlock)
        switchInfo.values.add(caseValueConverted as IntegerConstant)

        ir.switchLabel(caseBlock)
        visitStatement(caseStatement.stmt)
    }

    override fun visit(returnStatement: ReturnStatement) {
        if (ir.last() is TerminateInstruction) {
            return
        }
        val fnStmt = stmtStack.root()
        val expr = returnStatement.expr
        if (expr is EmptyExpression) {
            ir.branch(fnStmt.resolveExit(ir))
            return
        }
        val value = visitExpression(expr, true)
        when (val type = returnStatement.expr.resolveType(typeHolder)) {
            is CPrimitive, is CStringLiteral -> {
                val returnType = ir.prototype().returnType() as PrimitiveType
                val returnValue = ir.convertToType(value, returnType)
                ir.store(fnStmt.returnValueAdr(), returnValue)
            }
            is CStructType -> {
                ir.memcpy(fnStmt.returnValueAdr(), value, U64Value(type.size().toLong()))
            }
            else -> throw IRCodeGenError("Unknown return type, type=${returnStatement.expr.resolveType(typeHolder)}")
        }
        ir.branch(fnStmt.resolveExit(ir))
    }

    override fun visit(compoundStatement: CompoundStatement) = scoped {
        if (ir.last() is TerminateInstruction) {
            return@scoped
        }
        for (node in compoundStatement.statements) {
            when (node) {
                is Declaration -> visitDeclaration(node)
                is Statement   -> visitStatement(node)
                else -> throw IRCodeGenError("Statement expected")
            }
        }
    }

    override fun visit(ifStatement: IfStatement) = scoped {
        if (ir.last() is TerminateInstruction) {
            return@scoped
        }
        val condition = makeConditionFromExpression(ifStatement.condition)
        val thenBlock = ir.createLabel()

        if (ifStatement.elseNode is EmptyStatement) {
            val endBlock = ir.createLabel()
            ir.branchCond(condition, thenBlock, endBlock)
            ir.switchLabel(thenBlock)
            visitStatement(ifStatement.then)
            if (ir.last() !is TerminateInstruction) {
                ir.branch(endBlock)
            }
            ir.switchLabel(endBlock)
        } else {

            val elseBlock = ir.createLabel()
            ir.branchCond(condition, thenBlock, elseBlock)
            // then
            ir.switchLabel(thenBlock)
            visitStatement(ifStatement.then)
            val endBlock = if (ir.last() !is TerminateInstruction) {
                val endBlock = ir.createLabel()
                ir.branch(endBlock)
                endBlock
            } else {
                null
            }

            // else
            ir.switchLabel(elseBlock)
            visitStatement(ifStatement.elseNode)

            if (ir.last() !is TerminateInstruction) {
                val newEndBlock = endBlock ?: ir.createLabel()
                ir.branch(newEndBlock)
                ir.switchLabel(newEndBlock)
            } else if (endBlock != null) {
                ir.switchLabel(endBlock)
            }
        }
    }

    override fun visit(doWhileStatement: DoWhileStatement) = scoped {
        if (ir.last() is TerminateInstruction) {
            return@scoped
        }
        val bodyBlock = ir.createLabel()
        stmtStack.scoped(LoopStmtInfo()) { loopStmt ->
            ir.branch(bodyBlock)
            ir.switchLabel(bodyBlock)

            visitStatement(doWhileStatement.body)

            if (ir.last() !is TerminateInstruction) {
                val conditionBlock = loopStmt.resolveCondition(ir)
                ir.branch(conditionBlock)
                ir.switchLabel(conditionBlock)
                val condition = makeConditionFromExpression(doWhileStatement.condition)
                val endBlock = loopStmt.resolveExit(ir)
                ir.branchCond(condition, bodyBlock, endBlock)
                ir.switchLabel(endBlock)
            }
            if (loopStmt.exit() != null) {
                val exitBlock = loopStmt.resolveExit(ir)
                ir.switchLabel(exitBlock)
            }
        }
    }

    override fun visit(whileStatement: WhileStatement) = scoped {
        if (ir.last() is TerminateInstruction) {
            return@scoped
        }
        val bodyBlock = ir.createLabel()
        stmtStack.scoped(LoopStmtInfo()) { loopStmtInfo ->
            val conditionBlock = loopStmtInfo.resolveCondition(ir)
            ir.branch(conditionBlock)
            ir.switchLabel(conditionBlock)
            val condition = makeConditionFromExpression(whileStatement.condition)

            val endBlock = loopStmtInfo.resolveExit(ir)
            ir.branchCond(condition, bodyBlock, endBlock)
            ir.switchLabel(bodyBlock)
            visitStatement(whileStatement.body)
            if (ir.last() !is TerminateInstruction) {
                ir.branch(conditionBlock)
            }
            ir.switchLabel(endBlock)
        }
    }

    private fun visitInit(init: Node) = when (init) {
        is Declaration -> visitDeclaration(init)
        is ExprStatement -> visit(init)
        is EmptyStatement, is DummyNode -> {}
        else -> throw IRCodeGenError("Unknown init statement, init=$init")
    }

    private fun visitUpdate(update: Expression) {
        if (update is EmptyExpression) {
            return
        }

        visitExpression(update, true)
    }

    override fun visit(forStatement: ForStatement) = scoped {
        if (ir.last() is TerminateInstruction) {
            return@scoped
        }
        val bodyBlock = ir.createLabel()
        stmtStack.scoped(ForLoopStmtInfo()) { loopStmtInfo ->
            visitInit(forStatement.init)

            val conditionBlock = loopStmtInfo.resolveCondition(ir)
            ir.branch(conditionBlock)
            ir.switchLabel(conditionBlock)
            val cond = forStatement.condition
            if (cond is EmptyExpression) {
                ir.branch(bodyBlock)
            } else {
                val condition = makeConditionFromExpression(cond)
                val endBlock = loopStmtInfo.resolveExit(ir)
                ir.branchCond(condition, bodyBlock, endBlock)
            }

            if (ir.last() !is TerminateInstruction) {
                ir.branch(conditionBlock)
            }
            ir.switchLabel(bodyBlock)
            visitStatement(forStatement.body)
            if (ir.last() !is TerminateInstruction) {
                ir.branch(loopStmtInfo.resolveUpdate(ir))
            }
            val updateBB = loopStmtInfo.update()
            if (updateBB != null) {
                ir.switchLabel(updateBB)
                visitUpdate(forStatement.update)
                ir.branch(conditionBlock)
            }
            val endBlock = loopStmtInfo.resolveExit(ir)
            ir.switchLabel(endBlock)
        }
    }

    override fun visit(switchStatement: SwitchStatement) = scoped {
        if (ir.last() is TerminateInstruction) {
            return@scoped
        }
        val condition = visitExpression(switchStatement.condition, true)
        val conditionBlock = ir.currentLabel()

        stmtStack.scoped(SwitchStmtInfo(condition.type().asType(), arrayListOf(), arrayListOf())) { info ->
            visitStatement(switchStatement.body)
            if (info.exit() != null) {
                val endBlock = info.resolveExit(ir)
                if (ir.last() !is TerminateInstruction) {
                    ir.branch(endBlock)
                }
            }

            ir.switchLabel(conditionBlock)
            val default = info.default() ?: info.resolveExit(ir)
            ir.switch(condition, default, info.values, info.table)

            if (info.exit() != null) {
                ir.switchLabel(info.resolveExit(ir))
            }
        }
    }

    override fun visit(declarator: Declarator): Value {
        val type = typeHolder[declarator.name()]
        if (type.storageClass == StorageClass.STATIC) {
            return generateGlobalDeclarator(declarator)
        }

        val irType = when (val cType = type.type.cType()) {
            is CPrimitive     -> mb.toIRLVType<PrimitiveType>(typeHolder, cType)
            is CAggregateType -> mb.toIRType<NonTrivialType>(typeHolder, cType)
            else -> throw IRCodeGenError("Unknown type, type=$cType")
        }

        val rvalueAdr = ir.alloc(irType)
        varStack[declarator.name()] = rvalueAdr
        return rvalueAdr
    }

    private fun zeroingMemory(initializerList: InitializerList) {
        val value = initializerContext.peekValue()
        when (val type = initializerContext.peekType().cType()) {
            is CStructType -> {
                for (i in initializerList.initializers.size until type.fields().size) {
                    val converted = mb.toIRType<StructType>(typeHolder, type)
                    val elementType = converted.field(i)
                    val elementAdr = ir.gfp(value, converted, arrayOf(Constant.valueOf(Type.I64, i)))
                    ir.store(elementAdr, Constant.valueOf(elementType.asType(), 0))
                }
            }
            is CArrayType -> {
                if (initializerList.resolveType(typeHolder) is CStringLiteral) {
                    return
                }
                for (i in initializerList.initializers.size until type.dimension) {
                    val elementType = type.element()
                    val irElementType = mb.toIRType<NonTrivialType>(typeHolder, elementType.cType())
                    val elementAdr = ir.gep(value, irElementType, I64Value(i))
                    ir.store(elementAdr, Constant.valueOf(irElementType.asType(), 0))
                }
            }
            else -> throw IRCodeGenError("Unknown type, type=$type")
        }
    }

    private fun visitInitializerList(initializerList: InitializerList) {
        for ((idx, init) in initializerList.initializers.withIndex()) {
            when (init) {
                is SingleInitializer -> initializerContext.withIndex(idx) { visitSingleInitializer(init) }
                is DesignationInitializer -> initializerContext.withIndex(idx) { visitDesignationInitializer(init) }
            }
        }
        zeroingMemory(initializerList)
    }

    private fun visitDesignationInitializer(designationInitializer: DesignationInitializer) {
        val type = initializerContext.peekType().cType()
        val value = initializerContext.peekValue()
        for (designator in designationInitializer.designation.designators) {
            when (designator) {
                is ArrayDesignator -> {
                    val arrayType = type as CArrayType
                    val elementType = arrayType.element()
                    val converted = mb.toIRType<NonTrivialType>(typeHolder, elementType.cType())
                    val expression = visitExpression(designationInitializer.initializer, true)
                    val index = designator.constEval(typeHolder)
                    val convertedRvalue = ir.convertToType(expression, converted)
                    val elementAdr = ir.gep(value, converted, Constant.valueOf(Type.I64, index))
                    ir.store(elementAdr, convertedRvalue)
                }
                is MemberDesignator -> {
                    type as CStructType
                    val fieldType = mb.toIRType<StructType>(typeHolder, type)
                    val expression = visitExpression(designationInitializer.initializer, true)
                    val index = type.fieldIndex(designator.name()) ?: throw IRCodeGenError("Unknown field, field=${designator.name()}")
                    val converted = ir.convertToType(expression, fieldType.field(index))
                    val fieldAdr = ir.gfp(value, fieldType, arrayOf(Constant.valueOf(Type.I64, index)))
                    ir.store(fieldAdr, converted)
                }
            }
        }
    }

    override fun visit(initDeclarator: InitDeclarator): Value {
        val varDesc = typeHolder[initDeclarator.name()]
        if (varDesc.storageClass == StorageClass.STATIC) {
            return generateGlobalAssignmentDeclarator(initDeclarator)
        }
        val type = varDesc.type.cType()
        if (type !is CAggregateType) {
            val rvalue = visitExpression(initDeclarator.rvalue, true)
            val commonType = mb.toIRLVType<PrimitiveType>(typeHolder, type)
            val convertedRvalue = ir.convertToType(rvalue, commonType)

            val lvalueAdr = visit(initDeclarator.declarator)
            ir.store(lvalueAdr, convertedRvalue)
            return convertedRvalue
        }
        val lvalueAdr = initDeclarator.declarator.accept(this)
        when (val rvalue = initDeclarator.rvalue) {
            is InitializerList -> {
                initializerContext.scope(lvalueAdr, varDesc.type) { visitInitializerList(rvalue) }
                return lvalueAdr
            }
            is FunctionCall -> {
                val primary = rvalue.primary
                if (primary !is VarNode) {
                    throw IRCodeGenError("Unknown function call, primary=$primary")
                }
                val name = primary.name()
                val function = mb.findFunction(name) ?: throw IRCodeGenError("Function '$name' not found")
                val (convertedArgs, attributes) = convertFunctionArgs(function, rvalue.args)

                val cont = ir.createLabel()
                val call = when (function.returnType()) {
                    is PrimitiveType -> ir.call(function, convertedArgs, attributes, cont)
                    is TupleType     -> ir.tupleCall(function, convertedArgs, attributes, cont)
                    is StructType    -> ir.call(function, convertedArgs, attributes, cont)
                    is VoidType      -> {
                        ir.vcall(function, arrayListOf(lvalueAdr) + convertedArgs, attributes, cont)
                        lvalueAdr
                    }
                    else -> throw IRCodeGenError("Unknown type ${function.returnType()}")
                }
                ir.switchLabel(cont)

                when (val functionType = rvalue.resolveType(typeHolder)) {
                    is CStructType -> {
                        if (!functionType.isSmall()) {
                            return lvalueAdr
                        }
                        val structType = mb.toIRType<StructType>(typeHolder, functionType)
                        val list = CallConvention.coerceArgumentTypes(functionType) ?: throw IRCodeGenError("Unknown type, type=$functionType")
                        if (list.size == 1) {
                            val gep = ir.gep(lvalueAdr, structType,
                                Constant.of(Type.I64, 0)
                            )
                            ir.store(gep, call)
                        } else {
                            list.forEachIndexed { idx, arg ->
                                val offset   = (idx * QWORD_SIZE) / arg.sizeOf()
                                val fieldPtr = ir.gep(lvalueAdr, arg,
                                    Constant.valueOf(Type.I64, offset)
                                )
                                val proj = ir.proj(call, idx)
                                ir.store(fieldPtr, proj)
                            }
                        }
                        return lvalueAdr
                    }
                    else -> throw IRCodeGenError("Unknown type, type=$functionType")
                }
            }
            else -> {
                val rvalue = visitExpression(initDeclarator.rvalue, true)
                val commonType = mb.toIRType<NonTrivialType>(typeHolder, type)
                ir.memcpy(lvalueAdr, rvalue, U64Value(commonType.sizeOf().toLong()))
                return lvalueAdr
            }
        }
    }

    override fun visit(arrayDeclarator: ArrayDeclarator): Value {
        TODO("Not yet implemented")
    }

    override fun visit(emptyDeclarator: EmptyDeclarator): Value {
        return Value.UNDEF
    }

    override fun visit(structDeclarator: StructDeclarator): Value {
        TODO("Not yet implemented")
    }

    override fun visit(directDeclarator: DirectDeclarator): Value {
        TODO("Not yet implemented")
    }
}

class FunGenInitializer(moduleBuilder: ModuleBuilder,
                        typeHolder: TypeHolder,
                        varStack: VarStack<Value>,
                        nameGenerator: NameGenerator) : AbstractIRGenerator(moduleBuilder, typeHolder, varStack, nameGenerator) {
    fun generate(functionNode: FunctionNode) {
        val fnType     = functionNode.declareType(functionNode.specifier, typeHolder).type.asType<CFunctionType>()
        val parameters = functionNode.functionDeclarator().params()
        val irRetType  = irReturnType(fnType.retType())
        val (argTypes, attributes) = argumentTypes(fnType)

        if (fnType.variadic()) {
            attributes.add(VarArgAttribute)
        }
        val currentFunction = mb.createFunction(functionNode.name(), irRetType, argTypes, attributes)
        val funGen = IrGenFunction(mb, typeHolder, varStack, nameGenerator, currentFunction, fnType)
        funGen.visitFun(parameters, functionNode)
    }
}