package codegen

import types.*
import ir.types.*
import ir.value.*
import parser.nodes.*
import common.assertion
import codegen.TypeConverter.loadCoerceArguments
import codegen.TypeConverter.convertRVToType
import ir.instruction.*
import ir.module.block.Label
import codegen.TypeConverter.convertToType
import codegen.TypeConverter.storeCoerceArguments
import codegen.TypeConverter.toIRLVType
import codegen.TypeConverter.toIRType
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
import ir.module.DirectFunctionPrototype
import ir.module.IndirectFunctionPrototype
import ir.module.builder.impl.ModuleBuilder
import ir.module.builder.impl.FunctionDataBuilder
import ir.value.constant.*
import parser.LineAgnosticAstPrinter
import parser.nodes.visitors.DeclaratorVisitor
import parser.nodes.visitors.StatementVisitor
import tokenizer.Position
import typedesc.*


private data class FunctionArgInfo(val args: List<Value>, val attributes: Set<FunctionAttribute>)

private class IrGenFunction(moduleBuilder: ModuleBuilder,
                    typeHolder: TypeHolder,
                    varStack: VarStack<Value>,
                    nameGenerator: NameGenerator,
                    private val ir: FunctionDataBuilder,
                    private val functionType: CFunctionType) :
    AbstractIRGenerator(moduleBuilder, typeHolder, varStack, nameGenerator),
    StatementVisitor<Unit>,
    DeclaratorVisitor<Value> {
    private var stringToLabel = mutableMapOf<String, Label>()
    private val stmtStack = StmtStack()

    private val vaListIrType by lazy {
        mb.toIRType<StructType>(typeHolder, VaStart.vaList)
    }

    private inline fun<reified T> scoped(noinline block: () -> T): T {
        return typeHolder.scoped { varStack.scoped(block) }
    }

    private fun seekOrAddLabel(name: String): Label {
        return stringToLabel[name] ?: let {
            val newLabel = ir.createLabel()
            stringToLabel[name] = newLabel
            newLabel
        }
    }

    private fun visitDeclaration(declaration: Declaration) {
        declaration.specifyType(typeHolder, listOf())

        for (declarator in declaration.declarators()) {
            declarator.accept(this)
        }
    }

    private fun makeConditionFromExpression(condition: Expression): Value {
        val conditionExpr = visitExpression(condition, true)

        return when (val type = conditionExpr.type()) {
            is IntegerType       -> ir.icmp(conditionExpr, IntPredicate.Ne, IntegerConstant.of(type.asType(), 0))
            is PtrType           -> ir.icmp(conditionExpr, IntPredicate.Ne, NullValue)
            is FloatingPointType -> ir.fcmp(conditionExpr, FloatPredicate.One, FloatingPointConstant.of(type, 0))
            is FlagType          -> conditionExpr
            else -> throw RuntimeException("Unknown type: type=$type")
        }
    }

    private inline fun<reified T: AnyPredicateType> makeCondition(a: Value, predicate: T, b: Value): Value = when (val type = a.type()) {
        is IntegerType, PtrType -> ir.icmp(a, predicate as IntPredicate, b)
        is FloatingPointType    -> ir.fcmp(a, predicate as FloatPredicate, b)
        else -> throw RuntimeException("Unknown type: type=$type")
    }

    private fun visitCompoundLiteral(compoundLiteral: CompoundLiteral): Value {
        val type   = compoundLiteral.typeDesc(typeHolder)
        val irType = mb.toIRType<AggregateType>(typeHolder, type.cType())
        val adr    = ir.alloc(irType)
        visitInitializerList(compoundLiteral.initializerList, adr, type.asType())
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
        is CompoundLiteral   -> visitCompoundLiteral(expression)
        is BuiltinVaStart    -> visitBuiltInVaStart(expression)
        is BuiltinVaArg      -> visitBuiltInVaArg(expression)
        is BuiltinVaEnd      -> visitBuiltInVaEnd(expression)
        is BuiltinVaCopy     -> visitBuiltInVaCopy(expression)
        else -> throw RuntimeException("Unknown expression: $expression")
    }

    private fun visitBuiltInVaCopy(builtinVaCopy: BuiltinVaCopy): Value {
        val dest = visitExpression(builtinVaCopy.dest, true)
        val dstType = builtinVaCopy.dest.resolveType(typeHolder)
        if (dstType !== VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $dstType", builtinVaCopy.begin())
        }
        val src = visitExpression(builtinVaCopy.src, true)
        val srcType = builtinVaCopy.src.resolveType(typeHolder)
        if (srcType !== VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $srcType", builtinVaCopy.begin())
        }
        val irType = mb.toIRType<StructType>(typeHolder, srcType)
        val destPtr = ir.gep(dest, irType, I64Value.of(0))
        val srcPtr = ir.gep(src, irType, I64Value.of(0))
        ir.memcpy(destPtr, srcPtr, U64Value.of(irType.sizeOf().toLong()))
        return UndefValue
    }

    private fun visitBuiltInVaEnd(builtinVaEnd: BuiltinVaEnd): Value {
        val vaListType = builtinVaEnd.vaList.resolveType(typeHolder)
        if (vaListType !== VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $vaListType", builtinVaEnd.begin())
        }
        // Nothing to do
        return UndefValue
    }

    private fun visitBuiltInVaStart(builtinVaStart: BuiltinVaStart): Value {
        val vaList = visitExpression(builtinVaStart.vaList, true)
        val vaListType = builtinVaStart.vaList.resolveType(typeHolder)
        if (vaListType !== VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $vaListType", builtinVaStart.begin())
        }
        val fnStmt = stmtStack.root()
        val vaInit = fnStmt.vaInit as Alloc
        val cont = ir.createLabel()
        ir.intrinsic(arrayListOf(vaList, vaInit), VaStart(functionType.args()), cont)
        ir.switchLabel(cont)

        return UndefValue
    }

    private fun visitBuiltInVaArg(builtinVaArg: BuiltinVaArg): Value {
        val vaListType = builtinVaArg.assign.resolveType(typeHolder)
        if (vaListType !== VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $vaListType", builtinVaArg.begin())
        }

        val vaList = visitExpression(builtinVaArg.assign, true)
        return when (val argCType = builtinVaArg.resolveType(typeHolder)) {
            is CHAR, is UCHAR, is SHORT, is USHORT, is INT, is UINT, is LONG, is ULONG, is CPointer -> {
                val argType = mb.toIRType<PrimitiveType>(typeHolder, argCType)
                emitBuiltInVaArg(vaList, argType, VaStart.GP_OFFSET_IDX, VaStart.REG_SAVE_AREA_SIZE)
            }
            is DOUBLE, is FLOAT -> {
                val argType = mb.toIRType<PrimitiveType>(typeHolder, argCType)
                emitBuiltInVaArg(vaList, argType, VaStart.FP_OFFSET_IDX, VaStart.FP_REG_SAVE_AREA_SIZE)
            }
            is CStructType -> {
                val irType = mb.toIRType<StructType>(typeHolder, argCType)
                val alloc = ir.alloc(irType)
                if (!argCType.isSmall()) {
                    emitBuiltInVarArgLargeStruct(vaList, alloc, irType)
                    return alloc
                }
                val types = CallConvention.coerceArgumentTypes(argCType) ?: throw RuntimeException("Internal error")
                for ((idx, type) in types.withIndex()) {
                    val fieldPtr = ir.gep(alloc, I8Type, I64Value.of(type.sizeOf().toLong() * idx))
                    val arg = when (type) {
                        is PtrType, is IntegerType -> emitBuiltInVaArg(vaList, type, VaStart.GP_OFFSET_IDX, VaStart.REG_SAVE_AREA_SIZE)
                        is FloatingPointType           -> emitBuiltInVaArg(vaList, type, VaStart.FP_OFFSET_IDX, VaStart.FP_REG_SAVE_AREA_SIZE)
                        else -> throw IRCodeGenError("Unknown type $type", builtinVaArg.begin())
                    }
                    ir.store(fieldPtr, arg)
                }
                alloc
            }
            else -> throw IRCodeGenError("Unknown type $argCType", builtinVaArg.begin())
        }
    }

    private fun emitBuiltInVarArgLargeStruct(vaList: Value, dst: Alloc, argType: StructType) {
        val overflowArgAreaPtr = ir.gfp(vaList, vaListIrType, I64Value.of(VaStart.OVERFLOW_ARG_AREA_IDX))
        val argInMem = ir.load(PtrType, overflowArgAreaPtr)
        val inc = ir.gep(argInMem, I8Type, I64Value.of(argType.sizeOf().toLong()))
        ir.store(overflowArgAreaPtr, inc)

        ir.memcpy(dst, argInMem, U64Value.of(argType.sizeOf().toLong()))
    }

    private fun emitBuiltInVaArg(vaList: Value, argType: PrimitiveType, offsetIdx: Int, regSaveAreaIdx: Int): Value {
        val gpOffsetPtr = ir.gfp(vaList, vaListIrType, I64Value.of(offsetIdx))
        val gpOffset = ir.load(I32Type, gpOffsetPtr)
        val gpOffsetCvt = ir.convertToType(gpOffset, I64Type)

        val varArgInReg = ir.createLabel()
        val varArgInStack = ir.createLabel()
        val cont = ir.createLabel()

        val isReg = ir.icmp(gpOffsetCvt, IntPredicate.Le, I64Value.of(regSaveAreaIdx))
        ir.branchCond(isReg, varArgInReg, varArgInStack)

        val argInReg = ir.switchLabel(varArgInReg).let {
            val regSaveAreaPtr = ir.gfp(vaList, vaListIrType, I64Value.of(VaStart.REG_SAVE_AREA_IDX))
            val regSaveArea = ir.load(PtrType, regSaveAreaPtr)
            val argInReg = ir.gep(regSaveArea, I8Type, gpOffsetCvt)
            val newGPOffset = ir.add(gpOffsetCvt, I64Value.of(QWORD_SIZE))
            val asI32 = ir.trunc(newGPOffset, I32Type)
            ir.store(gpOffsetPtr, asI32)
            ir.branch(cont)

            argInReg
        }

        val argInMem = ir.switchLabel(varArgInStack).let {
            val overflowArgAreaPtr = ir.gfp(vaList, vaListIrType, I64Value.of(VaStart.OVERFLOW_ARG_AREA_IDX))
            val argInMem = ir.load(PtrType, overflowArgAreaPtr)
            val inc = ir.gep(argInMem, I64Type, I64Value.of(1))
            ir.store(overflowArgAreaPtr, inc)
            ir.branch(cont)

            argInMem
        }

        ir.switchLabel(cont)
        val argPtr = ir.phi(listOf(argInReg, argInMem), listOf(varArgInReg, varArgInStack))
        return ir.load(argType, argPtr)
    }

    private fun visitInitializer(singleInitializer: SingleInitializer): Value = when (val expr = singleInitializer.expr) {
        is InitializerList -> visitInitializer(expr.initializers[0] as SingleInitializer)
        else -> visitExpression(expr, true)
    }

    private fun visitSingleInitializer(expr: Expression, lvalueAdr: Value, type: CAggregateType, idx: Int) {
        when (expr) {
            is InitializerList -> when (type) {
                is CArrayType -> {
                    val t = type.element()
                    val irType = mb.toIRType<AggregateType>(typeHolder, t.cType())
                    val fieldPtr = ir.gep(lvalueAdr, irType, I64Value.of(idx))
                    visitInitializerList(expr, fieldPtr, t.asType())
                }
                is CStructType -> {
                    val t = type.fieldByIndexOrNull(idx) ?: throw IRCodeGenError("Field '$idx' not found", expr.begin())
                    val irType = mb.toIRType<AggregateType>(typeHolder, t.cType())
                    val fieldPtr = ir.gfp(lvalueAdr, irType, I64Value.of(idx))
                    visitInitializerList(expr, fieldPtr, t.asType())
                }
                else -> throw RuntimeException("Unknown type: type=$type")
            }
            is StringNode -> {
                if (type !is CArrayType) {
                    throw IRCodeGenError("Expect array type, but type=$type", expr.begin())
                }
                when (type.element().cType()) {
                    is CHAR, is UCHAR -> {
                        if (expr.data().isNotEmpty()) {
                            ir.memcpy(lvalueAdr, visitStringNode(expr), U64Value.of(expr.length()))
                            val gep = ir.gep(lvalueAdr, I8Type, I64Value.of(expr.length()))
                            ir.store(gep, I8Value.of(0))
                        } else {
                            val gep = ir.gep(lvalueAdr, I8Type, I64Value.of(0))
                            ir.store(gep, I8Value.of(0))
                        }
                    }
                    is CPointer -> {
                        val stringPtr = visitStringNode(expr)
                        val fieldPtr = ir.gep(lvalueAdr, I64Type, I64Value.of(idx))
                        ir.store(fieldPtr, stringPtr)
                    }
                    else -> throw IRCodeGenError("Unknown type $type", expr.begin())
                }
            }
            else -> when (type) {
                is AnyCArrayType -> {
                    val rvalue = visitExpression(expr, true)
                    val irType = mb.toIRType<AggregateType>(typeHolder, type)

                    val field = when (val irFieldType = irType.field(idx)) {
                        is PrimitiveType -> irFieldType
                        is StructType    -> irFieldType.field(0)
                        else -> throw IRCodeGenError("Unknown type $irFieldType", expr.begin())
                    }
                    val converted = ir.convertToType(rvalue, field)
                    val fieldPtr = ir.gfp(lvalueAdr, irType, I64Value.of(idx))
                    ir.store(fieldPtr, converted)
                }
                is AnyCStructType -> {
                    val rvalue = visitExpression(expr, true)
                    val irType = mb.toIRType<AggregateType>(typeHolder, type)
                    val irFieldType = irType.field(idx)
                    val converted = ir.convertToType(rvalue, irFieldType)
                    val fieldPtr = ir.gfp(lvalueAdr, irType, I64Value.of(idx))
                    ir.store(fieldPtr, converted)
                }
                is InitializerType -> TODO()
            }
        }
    }

    private fun visitCharNode(charNode: CharNode): Value {
        val charType  = charNode.resolveType(typeHolder)
        val charValue = I8Value.of(charNode.toInt().toByte())
        return ir.convertToType(charValue, mb.toIRType<PrimitiveType>(typeHolder, charType))
    }

    private fun generateIfElsePattern(commonType: PrimitiveType, conditional: Conditional): Value {
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

    private fun generateSelectPattern(conditional: Conditional, commonType: IntegerType): Value {
        val onTrue = constEvalExpression0(conditional.eTrue) ?:
            return generateIfElsePattern(commonType, conditional)

        val onFalse = constEvalExpression0(conditional.eFalse) ?:
            return generateIfElsePattern(commonType, conditional)

        val onTrueConstant  = IntegerConstant.of(commonType, onTrue)
        val onFalseConstant = IntegerConstant.of(commonType, onFalse)
        val condition = makeConditionFromExpression(conditional.cond)
        return ir.select(condition, commonType, onTrueConstant, onFalseConstant)
    }

    private fun visitConditional(conditional: Conditional): Value =
        when (val cType = conditional.resolveType(typeHolder)) {
            is VOID -> {
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
                UndefValue
            }
            is AnyCInteger, is CEnumType -> {
                val commonType = mb.toIRType<IntegerType>(typeHolder, cType)
                generateSelectPattern(conditional, commonType)
            }
            is AnyCFloat, is CPointer -> {
                val commonType = mb.toIRType<PrimitiveType>(typeHolder, cType)
                generateIfElsePattern(commonType, conditional)
            }
            is BOOL -> generateSelectPattern(conditional, U8Type)
            is AnyCStructType -> generateIfElsePattern(PtrType, conditional)
            else -> throw IRCodeGenError("Unknown type $cType", conditional.begin())
        }

    private fun visitArrowMemberAccess(arrowMemberAccess: ArrowMemberAccess, isRvalue: Boolean): Value {
        val struct   = visitExpression(arrowMemberAccess.primary, true)
        val cPointer = when (val ty = arrowMemberAccess.primary.resolveType(typeHolder)) {
            is AnyCArrayType -> ty.asPointer()
            is CPointer      -> ty
            else -> throw IRCodeGenError("Pointer type expected, but got $ty", arrowMemberAccess.begin())
        }
        val cStructType = cPointer.dereference(typeHolder)
        val structIRType = mb.toIRType<StructType>(typeHolder, cStructType)

        if (cStructType !is AnyCStructType) {
            throw IRCodeGenError("Struct type expected, but got '$cStructType'", arrowMemberAccess.begin())
        }
        val fieldName = arrowMemberAccess.fieldName()
        val member = cStructType.fieldByIndexOrNull(fieldName) ?: let {
            throw IRCodeGenError("Field not found: $fieldName", arrowMemberAccess.begin())
        }

        val gep = ir.gfp(struct, structIRType, I64Value.of(member.index))
        if (!isRvalue) {
            return gep
        }

        val memberType = member.cType()
        if (memberType is CAggregateType || memberType is AnyCFunctionType) {
            return gep
        }
        val memberIRType = mb.toIRLVType<PrimitiveType>(typeHolder, memberType)
        return ir.load(memberIRType, gep)
    }

    private fun visitMemberAccess(memberAccess: MemberAccess, isRvalue: Boolean): Value {
        val struct = visitExpression(memberAccess.primary, false) //TODO isRvalue???
        val structType = memberAccess.primary.resolveType(typeHolder)
        if (structType !is AnyCStructType) {
            throw IRCodeGenError("Struct type expected, but got '$structType'", memberAccess.begin())
        }
        val structIRType = mb.toIRType<StructType>(typeHolder, structType)

        val fieldName = memberAccess.memberName()
        val member = structType.fieldByIndexOrNull(fieldName) ?:
            throw IRCodeGenError("Field not found: '$fieldName'", memberAccess.begin())

        val gep = ir.gfp(struct, structIRType, I64Value.of(member.index))
        if (!isRvalue) {
            return gep
        }
        val memberType = member.cType()
        if (memberType is CAggregateType || memberType is AnyCFunctionType) {
            return gep
        }

        val memberIRType = mb.toIRLVType<PrimitiveType>(typeHolder, memberType)
        return ir.load(memberIRType, gep)
    }

    private fun visitSizeOf(sizeOf: SizeOf): Value = I64Value.of(sizeOf.constEval(typeHolder))

    private fun visitStringNode(stringNode: StringNode): Value {
        val stringLiteral = StringLiteralGlobalConstant(createStringLiteralName(), ArrayType(I8Type, stringNode.length()), stringNode.data())
        return mb.addConstant(stringLiteral)
    }

    private fun visitArrayAccess(arrayAccess: ArrayAccess, isRvalue: Boolean): Value {
        val index = visitExpression(arrayAccess.expr, true)
        val convertedIndex = ir.convertToType(index, I64Type)
        val array = visitExpression(arrayAccess.primary, true)

        val arrayType = arrayAccess.resolveType(typeHolder)
        val elementType = mb.toIRLVType<NonTrivialType>(typeHolder, arrayType)

        val adr = ir.gep(array, elementType, convertedIndex)
        if (!isRvalue) {
            return adr
        }
        if (arrayType is CAggregateType || arrayType is AnyCFunctionType) {
            return adr
        }
        return ir.load(elementType.asType(), adr)
    }

    private fun convertArg(function: AnyFunctionPrototype, argIdx: Int, expr: Value): Value {
        if (argIdx < function.arguments().size) {
            val cvt = function.argument(argIdx) ?: throw RuntimeException("Internal error")
            return ir.convertToType(expr, cvt)
        }

        if (!function.attributes.contains(VarArgAttribute)) {
            throw IRCodeGenError("Too many arguments in function call '${function.shortDescription()}'", Position.UNKNOWN) //TODO correct position
        }
        return when (expr.type()) {
            F32Type         -> ir.convertToType(expr, F64Type)
            I8Type, I16Type -> ir.convertToType(expr, I32Type)
            U8Type, U16Type -> ir.convertToType(expr, U32Type)
            FlagType        -> ir.convertToType(expr, I32Type)
            else -> expr
        }
    }

    private fun convertFunctionArgs(function: AnyFunctionPrototype, returnType: CType, args: List<Expression>): FunctionArgInfo {
        val convertedArgs = mutableListOf<Value>()
        val attributes = hashSetOf<FunctionAttribute>()

        var offset = 0
        if (returnType is AnyCStructType && !returnType.isSmall()) {
            offset += 1
        }
        for ((idx, argValue) in args.withIndex()) {
            val expr = visitExpression(argValue, true)
            when (val argCType = argValue.resolveType(typeHolder)) {
                is CPrimitive, is CFunctionType, is CUncompletedArrayType, is CStringLiteral -> {
                    val convertedArg = convertArg(function, idx + offset, expr)
                    convertedArgs.add(convertedArg)
                }
                is CArrayType -> {
                    val type = mb.toIRType<ArrayType>(typeHolder, argCType)
                    val convertedArg = ir.gep(expr, type.elementType(), I64Value.of(0))
                    convertedArgs.add(convertedArg)
                }
                is AnyCStructType -> {
                    if (argCType === VaStart.vaList) {
                        convertedArgs.add(expr)
                        continue
                    }
                    if (!argCType.isSmall()) {
                        val irType = mb.toIRType<StructType>(typeHolder, argCType)
                        attributes.add(ByValue(idx + offset, irType))
                    }
                    val argValues = ir.loadCoerceArguments(argCType, expr)
                    convertedArgs.addAll(argValues)
                    offset += argValues.size - 1
                }
                else -> throw IRCodeGenError("Unknown type, type=${argCType} in function call", argValue.begin())
            }
        }
        return FunctionArgInfo(convertedArgs, attributes)
    }

    private fun visitCast(cast: Cast): Value {
        val value = visitExpression(cast.cast, true)
        return when (val castToType = cast.resolveType(typeHolder)) {
            is VOID -> value
            is BOOL -> ir.convertToType(value, FlagType)
            is CPointer  -> {
                val baseAddr = when (val fromType = cast.cast.resolveType(typeHolder)) {
                    is CArrayType -> {
                        val irType = mb.toIRType<ArrayType>(typeHolder, fromType)
                        ir.gep(value, irType.elementType(), I64Value.of(0))
                    }
                    is AnyCStructType -> {
                        val irType = mb.toIRType<StructType>(typeHolder, fromType)
                        ir.gfp(value, irType, I64Value.of(0))
                    }
                    is CPrimitive, is AnyCFunctionType, is CStringLiteral -> value
                    is CUncompletedArrayType -> TODO()
                    is InitializerType -> TODO()
                    is CUncompletedType -> TODO()
                    is TypeDef -> TODO()
                }
                ir.convertToType(baseAddr, PtrType)
            }
            is CPrimitive -> {
                val toType = mb.toIRType<PrimitiveType>(typeHolder, castToType)
                ir.convertToType(value, toType)
            }
            else -> throw IRCodeGenError("Unknown type $castToType", cast.begin())
        }
    }

    private fun icallFunctionForStructType(retValue: Value, loadedFunctionPtr: Value, returnType: AnyCStructType, function: IndirectFunctionPrototype, argInfo: FunctionArgInfo) {
        val cont = ir.createLabel()
        when (function.returnType()) {
            is PrimitiveType -> {
                val ret = ir.icall(loadedFunctionPtr, function, argInfo.args, argInfo.attributes, cont)
                ir.switchLabel(cont)
                val structType = mb.toIRType<StructType>(typeHolder, returnType)
                val gep = ir.gfp(retValue, structType, I64Value.of(0L))
                ir.store(gep, ret)
            }
            is TupleType -> {
                val tuple = ir.itupleCall(loadedFunctionPtr, function, argInfo.args, argInfo.attributes, cont)
                ir.switchLabel(cont)
                copyTuple(retValue, tuple, returnType)
            }
            is VoidType -> {
                ir.ivcall(loadedFunctionPtr, function, arrayListOf(retValue) + argInfo.args, argInfo.attributes, cont)
                ir.switchLabel(cont)
            }
            else -> throw RuntimeException("Unknown type ${function.returnType()}")
        }
    }

    private fun visitFunPointerCall(funcPointerCall: FunctionCall): Value {
        val functionType = funcPointerCall.functionType(typeHolder)
        val loadedFunctionPtr = visitExpression(funcPointerCall.primary, true)

        val cPrototype = CFunctionPrototypeBuilder(funcPointerCall.begin(), functionType, mb, typeHolder, StorageClass.AUTO).build()

        val prototype = IndirectFunctionPrototype(cPrototype.returnType, cPrototype.argumentTypes, cPrototype.attributes)
        val argInfo = convertFunctionArgs(prototype, functionType.retType().cType(), funcPointerCall.args)

        return when (val functionReturnType = functionType.retType().cType()) {
            VOID -> {
                val cont = ir.createLabel()
                ir.ivcall(loadedFunctionPtr, prototype, argInfo.args, argInfo.attributes, cont)
                ir.switchLabel(cont)
                UndefValue
            }
            is CPrimitive -> {
                val cont = ir.createLabel()
                val ret = ir.icall(loadedFunctionPtr, prototype, argInfo.args, argInfo.attributes, cont)
                ir.switchLabel(cont)
                ret
            }
            is CStructType -> {
                val retType = mb.toIRType<NonTrivialType>(typeHolder, functionReturnType)
                val retValue = ir.alloc(retType)
                icallFunctionForStructType(retValue, loadedFunctionPtr, functionReturnType, prototype, argInfo)
                retValue
            }
            else -> throw IRCodeGenError("Unknown type ${functionType.retType()}", funcPointerCall.begin())
        }
    }

    private fun callFunctionForStructType(lvalueAdr: Value, returnType: AnyCStructType, function: DirectFunctionPrototype, argInfo: FunctionArgInfo) {
        val cont = ir.createLabel()
        when (function.returnType()) {
            is PrimitiveType -> {
                val call = ir.call(function, argInfo.args, argInfo.attributes, cont)
                ir.switchLabel(cont)
                val gep = ir.gep(lvalueAdr, I8Type, I64Value.of(0L))
                ir.store(gep, call)
            }
            is TupleType -> {
                val call = ir.tupleCall(function, argInfo.args, argInfo.attributes, cont)
                ir.switchLabel(cont)
                copyTuple(lvalueAdr, call, returnType)
            }
            is VoidType -> {
                ir.vcall(function, arrayListOf(lvalueAdr) + argInfo.args, argInfo.attributes, cont)
                ir.switchLabel(cont)
            }
            else -> throw RuntimeException("Unknown type ${function.returnType()}")
        }
    }

    private fun visitFunctionCall(functionCall: FunctionCall): Value {
        val primary = functionCall.primary
        if (primary !is VarNode) {
            return visitFunPointerCall(functionCall)
        }
        val function = mb.findFunction(primary.name()) ?: return visitFunPointerCall(functionCall)
        val argInfo = convertFunctionArgs(function, functionType.retType().cType(), functionCall.args)

        return when (val functionType = functionCall.resolveType(typeHolder)) {
            VOID -> {
                val cont = ir.createLabel()
                ir.vcall(function, argInfo.args, argInfo.attributes, cont)
                ir.switchLabel(cont)
                UndefValue
            }
            is CPrimitive -> {
                val cont = ir.createLabel()
                val call = ir.call(function, argInfo.args, argInfo.attributes, cont)
                ir.switchLabel(cont)
                call
            }
            is CStructType -> {
                val retType = mb.toIRType<NonTrivialType>(typeHolder, functionType)
                val retValue = ir.alloc(retType)
                callFunctionForStructType(retValue, functionType, function, argInfo)
                retValue
            }

            else -> throw IRCodeGenError("Unknown type $functionType", functionCall.begin())
        }
    }

    private fun copyTuple(dst: Value, src: TupleValue, returnType: AnyCStructType) {
        val projections = arrayListOf<Projection>()
        for (idx in src.type().innerTypes().indices) {
            projections.add(ir.proj(src, idx))
        }

        ir.storeCoerceArguments(returnType, dst, projections)
    }

    private fun visitFunPointerForStructType(lvalueAdr: Value, returnType: AnyCStructType, funcPointerCall: FunctionCall) {
        val functionType = funcPointerCall.functionType(typeHolder)
        val loadedFunctionPtr = visitExpression(funcPointerCall.primary, true)

        val cPrototype = CFunctionPrototypeBuilder(funcPointerCall.begin(), functionType, mb, typeHolder, StorageClass.AUTO).build()

        val prototype = IndirectFunctionPrototype(cPrototype.returnType, cPrototype.argumentTypes, cPrototype.attributes)
        val argInfo = convertFunctionArgs(prototype, functionType.retType().cType(), funcPointerCall.args)

        icallFunctionForStructType(lvalueAdr, loadedFunctionPtr, returnType, prototype, argInfo)
    }

    private fun visitFuncCallForStructType(lvalueAdr: Value, returnType: AnyCStructType, rvalue: FunctionCall) {
        val primary = rvalue.primary
        if (primary !is VarNode) {
            return visitFunPointerForStructType(lvalueAdr, returnType, rvalue)
        }
        val functionPrototype = mb.findFunction(primary.name()) ?: return visitFunPointerForStructType(lvalueAdr, returnType, rvalue)
        val argInfo = convertFunctionArgs(functionPrototype, returnType, rvalue.args)

        callFunctionForStructType(lvalueAdr, returnType, functionPrototype, argInfo)
    }

    private fun eq(type: PrimitiveType): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Eq
        is FloatingPointType -> FloatPredicate.Oeq
        is PtrType           -> IntPredicate.Eq
        is UndefType         -> throw RuntimeException("Unsupported type: type=$type")
    }

    private fun ne(type: PrimitiveType): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Ne
        is FloatingPointType -> FloatPredicate.One
        is PtrType           -> IntPredicate.Ne
        is UndefType         -> throw RuntimeException("Unsupported type: type=$type")
    }

    private fun gt(type: PrimitiveType): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Gt
        is FloatingPointType -> FloatPredicate.Ogt
        is PtrType           -> IntPredicate.Gt
        is UndefType         -> throw RuntimeException("Unsupported type: type=$type")
    }

    private fun lt(type: PrimitiveType): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Lt
        is FloatingPointType -> FloatPredicate.Olt
        is PtrType           -> IntPredicate.Lt
        is UndefType         -> throw RuntimeException("Unsupported type: type=$type")
    }

    private fun le(type: PrimitiveType): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Le
        is FloatingPointType -> FloatPredicate.Ole
        is PtrType           -> IntPredicate.Le
        is UndefType         -> throw RuntimeException("Unsupported type: type=$type")
    }

    private fun ge(type: PrimitiveType): AnyPredicateType = when (type) {
        is IntegerType       -> IntPredicate.Ge
        is FloatingPointType -> FloatPredicate.Oge
        is PtrType           -> IntPredicate.Ge
        is UndefType         -> throw RuntimeException("Unsupported type: type=$type")
    }

    private fun makeAlgebraicBinary(binOp: BinaryOp, op: (a: Value, b: Value) -> Value): Value {
        when (val commonType = mb.toIRType<Type>(typeHolder, binOp.resolveType(typeHolder))) {
            is PtrType -> {
                val lvalue     = visitExpression(binOp.left, true)
                val lValueType = when (val l = binOp.left.resolveType(typeHolder)) {
                    is AnyCArrayType -> l.asPointer()
                    is CPointer      -> l
                    else -> throw IRCodeGenError("Pointer type expected, but got $l", binOp.begin())
                }
                val convertedLValue = ir.convertToType(lvalue, U64Type)

                val rvalue = visitExpression(binOp.right, true)
                when (val r = binOp.right.resolveType(typeHolder)) {
                    is AnyCArrayType, is CPrimitive -> {}
                    else -> throw IRCodeGenError("Primitive type expected, but got $r", binOp.begin())
                }
                val convertedRValue = ir.convertToType(rvalue, U64Type)

                val size = lValueType.dereference(typeHolder).size()
                val mul = ir.mul(convertedRValue, U64Value.of(size.toLong()))

                val result = op(convertedLValue, mul)
                return ir.convertRVToType(result, commonType)
            }
            is FloatingPointType -> {
                val left = visitExpression(binOp.left, true)
                val leftConverted = ir.convertRVToType(left, commonType)

                val right = visitExpression(binOp.right, true)
                val rightConverted = ir.convertRVToType(right, commonType)

                return op(leftConverted, rightConverted)
            }
            is IntegerType -> {
                val cvtType = when (commonType) {
                    is SignedIntType   -> if (commonType.sizeOf() < WORD_SIZE) I32Type else commonType
                    is UnsignedIntType -> if (commonType.sizeOf() < WORD_SIZE) U32Type else commonType
                    else -> throw IRCodeGenError("Unexpected type: $commonType", binOp.begin())
                }
                val left = visitExpression(binOp.left, true)
                val leftConverted = ir.convertToType(left, cvtType)

                val right = visitExpression(binOp.right, true)
                val rightConverted = ir.convertToType(right, cvtType)

                return op(leftConverted, rightConverted)
            }
            is FlagType -> {
                val left = visitExpression(binOp.left, true)
                val leftConverted = ir.convertToType(left, I32Type)

                val right = visitExpression(binOp.right, true)
                val rightConverted = ir.convertToType(right, I32Type)

                return op(leftConverted, rightConverted)
            }
            is UndefType -> throw IRCodeGenError("Undef type", binOp.begin())
            else -> throw IRCodeGenError("Unexpected type: $commonType", binOp.begin())
        }
    }

    private fun makeAlgebraicBinaryWithAssignment(binOp: BinaryOp, op: (a: Value, b: Value) -> Value): Value {
        when (val commonType = mb.toIRType<NonTrivialType>(typeHolder, binOp.resolveType(typeHolder))) {
            is PtrType -> {
                val rvalue     = visitExpression(binOp.right, true)
                val rValueType = binOp.right.resolveType(typeHolder)
                if (rValueType !is CPrimitive) {
                    throw IRCodeGenError("Primitive type expected, but got $rValueType", binOp.begin())
                }
                val convertedRValue = ir.convertToType(rvalue, U64Type)

                val lvalueAddress = visitExpression(binOp.left, false)
                val lValueType    = binOp.left.resolveType(typeHolder)
                val lvalue        = ir.load(PtrType, lvalueAddress)
                val ptr2intLValue = ir.ptr2int(lvalue, U64Type)

                if (lValueType !is CPointer) {
                    throw IRCodeGenError("Pointer type expected, but got $lValueType", binOp.begin())
                }
                val convertedLValue = ir.convertToType(ptr2intLValue, U64Type)

                val size = lValueType.dereference(typeHolder).size()
                val mul = ir.mul(convertedRValue, U64Value.of(size.toLong()))

                val result = op(convertedLValue, mul)
                val res = ir.convertToType(result, commonType)
                ir.store(lvalueAddress, res)
                return res
            }
            is FloatingPointType -> {
                val right = visitExpression(binOp.right, true)
                val leftType = binOp.left.resolveType(typeHolder)
                val leftIrType = mb.toIRType<PrimitiveType>(typeHolder, leftType)
                val rightConverted = ir.convertToType(right, leftIrType)

                val left = visitExpression(binOp.left, false)
                val loadedLeft = ir.load(leftIrType, left)

                val sum = op(loadedLeft, rightConverted)
                ir.store(left, sum)
                return sum
            }
            is IntegerType -> {
                val right = visitExpression(binOp.right, true)
                val leftType = binOp.left.resolveType(typeHolder)
                val originalIrType = mb.toIRType<IntegerType>(typeHolder, leftType)
                val leftIrType = when (originalIrType) {
                    is SignedIntType -> if (originalIrType.sizeOf() < WORD_SIZE) I32Type else originalIrType
                    is UnsignedIntType -> if (originalIrType.sizeOf() < WORD_SIZE) U32Type else originalIrType
                }
                val rightConverted = ir.convertToType(right, leftIrType)

                val left = visitExpression(binOp.left, false)
                val loadedLeft = ir.load(originalIrType, left)
                val cvtLft = ir.convertToType(loadedLeft, leftIrType)

                val sum = op(cvtLft, rightConverted)
                val sumCvt = ir.convertToType(sum, originalIrType)
                ir.store(left, sumCvt)
                return sum
            }
            else -> throw RuntimeException("Unknown type: type=$commonType")
        }
    }

    private inline fun makeComparisonBinary(binOp: BinaryOp, crossinline predicate: (PrimitiveType) -> AnyPredicateType, isRvalue: Boolean): Value {
        val commonType = mb.toIRType<Type>(typeHolder, binOp.resolveType(typeHolder))
        val left = visitExpression(binOp.left, true)
        val leftConverted = ir.convertRVToType(left, commonType)

        val right = visitExpression(binOp.right, true)
        val rightConverted = ir.convertRVToType(right, commonType)

        val predicateType = when (commonType) {
            is FlagType      -> I8Type
            is PrimitiveType -> commonType
            else -> throw RuntimeException("Unknown type: type=$commonType")
        }

        val cmp = makeCondition(leftConverted, predicate(predicateType), rightConverted)
        if (isRvalue) {
            return cmp
        }
        return ir.convertToType(cmp, I8Type)
    }

    private fun visitAssignBinary(binop: BinaryOp): Value {
        val rightExpression = binop.right
        val leftType = binop.left.resolveType(typeHolder)
        if (leftType is AnyCStructType && rightExpression is FunctionCall) {
            val lvalueAdr = visitExpression(binop.left, false)
            visitFuncCallForStructType(lvalueAdr, leftType, rightExpression)
            return lvalueAdr
        }

        val right = visitExpression(binop.right, true)
        if (leftType !is AnyCStructType) {
            val leftIrType = mb.toIRType<Type>(typeHolder, leftType)
            val rightCvt = ir.convertRVToType(right, leftIrType)

            val left = visitExpression(binop.left, false)
            ir.store(left, rightCvt)
            return rightCvt
        }

        val left = visitExpression(binop.left, true)
        ir.memcpy(left, right, U64Value.of(leftType.size().toLong()))
        return right
    }

    private fun cvtToI8(right: Value): Value = when (val ty = right.type()) {
        is FlagType -> ir.convertToType(right, I8Type)
        is IntegerType -> {
            val cmp = ir.icmp(right, IntPredicate.Ne, IntegerConstant.of(ty, 0))
            ir.convertToType(cmp, I8Type)
        }
        is PtrType -> {
            val cmp = ir.icmp(right, IntPredicate.Ne, NullValue)
            ir.convertToType(cmp, I8Type)
        }
        else -> throw RuntimeException("Unknown type: type=$ty")
    }

    private fun visitBinary(binOp: BinaryOp, isRvalue: Boolean): Value = when (binOp.opType) { // TODO can compile incorrect code
        BinaryOpType.ADD -> makeAlgebraicBinary(binOp, ir::add)
        BinaryOpType.SUB -> makeAlgebraicBinary(binOp, ir::sub)
        BinaryOpType.ASSIGN -> visitAssignBinary(binOp)
        BinaryOpType.ADD_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::add)
        BinaryOpType.DIV_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ::divide)
        BinaryOpType.MUL_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::mul)
        BinaryOpType.BIT_OR -> makeAlgebraicBinary(binOp, ir::or)
        BinaryOpType.MUL -> makeAlgebraicBinary(binOp, ir::mul)
        BinaryOpType.NE -> makeComparisonBinary(binOp, ::ne, isRvalue)
        BinaryOpType.GT -> makeComparisonBinary(binOp, ::gt, isRvalue)
        BinaryOpType.LT -> makeComparisonBinary(binOp, ::lt, isRvalue)
        BinaryOpType.LE -> makeComparisonBinary(binOp, ::le, isRvalue)
        BinaryOpType.AND -> {
            val left = visitExpression(binOp.left, true)
            val convertedLeft = ir.convertToType(left, FlagType)

            val bb = ir.createLabel()
            val end = ir.createLabel()
            val initialBB = ir.currentLabel()
            ir.branchCond(convertedLeft, bb, end)
            ir.switchLabel(bb)

            val right = visitExpression(binOp.right, true)
            val convertedRight = cvtToI8(right)
            val current = ir.currentLabel()
            ir.branch(end)
            ir.switchLabel(end)
            ir.phi(listOf(I8Value.of(0), convertedRight), listOf(initialBB, current))
        }
        BinaryOpType.OR -> {
            val left = visitExpression(binOp.left, true)
            val convertedLeft = ir.convertToType(left, FlagType)

            val initialBB = ir.currentLabel()
            val bb = ir.createLabel()
            val end = ir.createLabel()
            ir.branchCond(convertedLeft, end, bb)
            ir.switchLabel(bb)

            val right = visitExpression(binOp.right, true)
            val convertedRight = cvtToI8(right)

            val current = ir.currentLabel()
            ir.branch(end)
            ir.switchLabel(end)
            ir.phi(listOf(I8Value.of(1), convertedRight), listOf(initialBB, current))
        }
        BinaryOpType.SHR_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::shr)
        BinaryOpType.SHL_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::shl)
        BinaryOpType.BIT_XOR_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::xor)
        BinaryOpType.BIT_OR_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::or)
        BinaryOpType.GE  -> makeComparisonBinary(binOp, ::ge, isRvalue)
        BinaryOpType.EQ  -> makeComparisonBinary(binOp, ::eq, isRvalue)
        BinaryOpType.SHL -> makeAlgebraicBinary(binOp, ir::shl)
        BinaryOpType.SHR -> makeAlgebraicBinary(binOp, ir::shr)
        BinaryOpType.BIT_AND -> makeAlgebraicBinary(binOp, ir::and)
        BinaryOpType.BIT_XOR -> makeAlgebraicBinary(binOp, ir::xor)
        BinaryOpType.MOD -> makeAlgebraicBinary(binOp, ::rem)
        BinaryOpType.DIV -> makeAlgebraicBinary(binOp, ::divide)
        BinaryOpType.SUB_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::sub)
        BinaryOpType.MOD_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ::rem)
        BinaryOpType.BIT_AND_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::and)
        BinaryOpType.COMMA -> {
            visitExpression(binOp.left, false)
            visitExpression(binOp.right, false)
        }
    }

    private fun divide(a: Value, b: Value): Value = when (val type = a.type()) {
        is IntegerType -> {
            val tupleDiv = ir.tupleDiv(a, b)
            ir.proj(tupleDiv, 0)
        }
        is FloatingPointType -> ir.div(a, b)
        else -> throw RuntimeException("Unknown type: type=$type")
    }

    private fun rem(a: Value, b: Value): Value = when (val type = a.type()) {
        is IntegerType -> {
            val tupleDiv = ir.tupleDiv(a, b)
            ir.proj(tupleDiv, 1)
        }
        else -> throw RuntimeException("Unknown type: type=$type")
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
                val converted = ir.convertToType(loaded, I64Type)
                val inc = op(converted, I64Value.of(ctype.dereference(typeHolder).size().toLong()))
                ir.store(addr, ir.convertToType(inc, type))
            }
            is CPrimitive -> {
                val inc = op(loaded, PrimitiveConstant.of(loaded.type(), 1))
                ir.store(addr, ir.convertToType(inc, type))
            }
            else -> throw IRCodeGenError("Unknown type: $ctype", unaryOp.begin())
        }
        return loaded
    }

    private fun visitPrefixIncOrDec(unaryOp: UnaryOp, op: (a: Value, b: Value) -> LocalValue): Value {
        assertion(unaryOp.opType == PrefixUnaryOpType.INC || unaryOp.opType == PrefixUnaryOpType.DEC) {
            "Unknown operation, op=${unaryOp.opType}"
        }

        val address = visitExpression(unaryOp.primary, false)
        when (val cType = unaryOp.resolveType(typeHolder)) {
            is CPointer -> {
                val loaded    = ir.load(PtrType, address)
                val converted = ir.convertToType(loaded, I64Type)
                val inc       = op(converted, I64Value.of(cType.dereference(typeHolder).size().toLong()))
                val incPtr    = ir.convertToType(inc, PtrType)
                ir.store(address, incPtr)
                return incPtr
            }
            is CPrimitive -> {
                val type   = mb.toIRType<PrimitiveType>(typeHolder, cType)
                val loaded = ir.load(type, address)
                val inc    = op(loaded, PrimitiveConstant.of(loaded.type(), 1))
                ir.store(address, ir.convertToType(inc, type))
                return inc
            }
            else -> throw IRCodeGenError("Unknown type: $cType", unaryOp.begin())
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
                if (type is CAggregateType || type is AnyCFunctionType) {
                    return addr
                }
                val loadedType = mb.toIRLVType<PrimitiveType>(typeHolder, type)
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
                val commonType = mb.toIRType<Type>(typeHolder, type)
                val converted = ir.convertRVToType(value, commonType) //TODO do we need this?

                val eqType = when (commonType) {
                    is FlagType      -> I8Type
                    is PrimitiveType -> commonType
                    else -> throw IRCodeGenError("Unknown type: $commonType", unaryOp.begin())
                }

                makeCondition(converted, eq(eqType), PrimitiveConstant.of(converted.asType(), 0))
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
        if (type is CAggregateType || type is AnyCFunctionType) {
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
            return I32Value.of(enumValue)
        }

        throw IRCodeGenError("Variable '$name' not found", varNode.begin())
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
                    val types = CallConvention.coerceArgumentTypes(cType) ?: listOf(PtrType)
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
                else -> throw IRCodeGenError("Unknown type, type=$cType", Position.UNKNOWN) //TODO correct position
            }
            argumentIdx++
        }
    }

    private fun visitParameter(param: String, cType: CType, args: List<ArgumentValue>) {
        when (cType) {
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
                if (!cType.isSmall() || cType === VaStart.vaList) {
                    assertion(args.size == 1) { "invariant" }
                    varStack[param] = args[0]
                    return
                }

                val irType    = mb.toIRType<NonTrivialType>(typeHolder, cType)
                val rvalueAdr = ir.alloc(irType)
                ir.storeCoerceArguments(cType, rvalueAdr, args)
                varStack[param] = rvalueAdr
            }
            else -> throw IRCodeGenError("Unknown type, type=$cType", Position.UNKNOWN) //TODO correct position
        }
    }

    private fun emitReturnType(fnStmt: FunctionStmtInfo, retCType: TypeDesc, args: List<ArgumentValue>) {
        val exitBlock = fnStmt.resolveExit(ir)
        when (val cType = retCType.cType()) {
            is VOID -> {
                ir.switchLabel(exitBlock)
                ir.retVoid()
            }
            is BOOL -> {
                val returnValueAdr = fnStmt.resolveReturnValueAdr { ir.alloc(I8Type) }
                ir.switchLabel(exitBlock)
                val ret = ir.load(I8Type, returnValueAdr)
                ir.ret(I8Type, arrayOf(ret))
            }
            is CPrimitive -> {
                val retType = mb.toIRLVType<PrimitiveType>(typeHolder, retCType.cType())
                val returnValueAdr = fnStmt.resolveReturnValueAdr { ir.alloc(retType) }
                ir.switchLabel(exitBlock)
                val ret = ir.load(retType, returnValueAdr)
                ir.ret(retType, arrayOf(ret))
            }
            is AnyCStructType -> when (val irRetType = ir.prototype().returnType()) {
                is PrimitiveType -> {
                    val returnValueAdr = fnStmt.resolveReturnValueAdr {
                        ir.alloc(mb.toIRType<StructType>(typeHolder, cType))
                    }
                    ir.switchLabel(exitBlock)

                    ir.ret(irRetType, arrayOf(returnValueAdr))
                }
                is TupleType -> {
                    val returnValueAdr = fnStmt.resolveReturnValueAdr {
                        ir.alloc(mb.toIRType<StructType>(typeHolder, cType))
                    }
                    ir.switchLabel(exitBlock)

                    val retValues = ir.loadCoerceArguments(cType, returnValueAdr)
                    assertion(retValues.size > 1) { "Internal error" }
                    ir.ret(irRetType, retValues.toTypedArray())
                }
                is VoidType -> {
                    assertion(!cType.isSmall()) { "Internal error" }
                    fnStmt.resolveReturnValueAdr { args[0] }
                    ir.switchLabel(exitBlock)
                    ir.retVoid()
                }
                else -> throw RuntimeException("Unknown type, type=$irRetType")
            }
            else -> throw RuntimeException("Unknown return type, type=$cType")
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
            val retType = functionNode.resolveType(typeHolder).retType().cType()
            val arguments = if (retType is AnyCStructType && !retType.isSmall()) {
                ir.arguments().takeLast(parameters.size)
            } else {
                ir.arguments()
            }
            visitParameters(parameters, functionType.args(), arguments) { param, cType, args ->
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
            throw IRCodeGenError("Goto statement outside of labeled statement", gotoStatement.begin())
        }

        val label = seekOrAddLabel(gotoStatement.id.str())
        ir.branch(label)
    }

    override fun visit(continueStatement: ContinueStatement) {
        if (ir.last() is TerminateInstruction) {
            return
        }

        when (val loopInfo = stmtStack.topLoop()) {
            is ForLoopStmtInfo -> ir.branch(loopInfo.resolveUpdate(ir)) //TODO bug???
            is LoopStmtInfo    -> ir.branch(loopInfo.resolveCondition(ir))
            else -> throw IRCodeGenError("Continue statement outside of loop", continueStatement.begin())
        }
    }

    override fun visit(breakStatement: BreakStatement) {
        if (ir.last() is TerminateInstruction) {
            return
        }

        val loopInfo = stmtStack.topSwitchOrLoop() ?:
            throw IRCodeGenError("Break statement outside of loop or switch", breakStatement.begin())
        ir.branch(loopInfo.resolveExit(ir))
    }

    override fun visit(defaultStatement: DefaultStatement) = scoped {
        val switchInfo = stmtStack.top() as SwitchStmtInfo
        val default = switchInfo.resolveDefault(ir)

        if (ir.last() !is TerminateInstruction && ir.currentLabel() != switchInfo.condBlock) {
            ir.branch(default)
        }

        ir.switchLabel(default)
        visitStatement(defaultStatement.stmt)
    }

    override fun visit(caseStatement: CaseStatement) = scoped {
        val switchInfo = stmtStack.top() as SwitchStmtInfo

        val ctx = CommonConstEvalContext<Int>(typeHolder)
        val constant = ConstEvalExpression.eval(caseStatement.constExpression, TryConstEvalExpressionInt(ctx))
            ?: throw IRCodeGenError("Case statement with non-constant expression: ${LineAgnosticAstPrinter.print(caseStatement.constExpression)}", caseStatement.begin())

        val caseValueConverted = IntegerConstant.of(switchInfo.conditionType.asType(), constant)
        val caseBlock = ir.createLabel()
        if (switchInfo.table.isNotEmpty() && ir.last() !is TerminateInstruction) {
            // fall through
            ir.branch(caseBlock)
        }

        switchInfo.table.add(caseBlock)
        switchInfo.values.add(caseValueConverted)

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
            is CPrimitive, is CStringLiteral -> when (functionType.retType().cType()) {
                is BOOL -> {
                    val returnType = ir.prototype().returnType().asType<PrimitiveType>()
                    val returnValue = ir.convertToType(value, FlagType)
                    val cvt = ir.convertToType(returnValue, returnType)
                    ir.store(fnStmt.returnValueAdr(), cvt)
                }
                is CPrimitive -> {
                    val returnType = ir.prototype().returnType().asType<PrimitiveType>()
                    val returnValue = ir.convertToType(value, returnType)
                    ir.store(fnStmt.returnValueAdr(), returnValue)
                }
                else -> throw RuntimeException("internal error")
            }
            is AnyCStructType -> {
                ir.memcpy(fnStmt.returnValueAdr(), value, U64Value.of(type.size().toLong()))
            }
            is AnyCArrayType -> {
                val returnType = ir.prototype().returnType().asType<PtrType>()
                val returnValue = ir.convertToType(value, returnType)
                ir.store(fnStmt.returnValueAdr(), returnValue)
            }
            else -> throw IRCodeGenError("Unknown return type, type=$type", returnStatement.begin())
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
                else -> throw IRCodeGenError("Statement or declaration expected, but got $node", node.begin())
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

    private fun visitInit(init: Node?) = when (init) {
        is Declaration -> visitDeclaration(init)
        is ExprStatement -> visit(init)
        is EmptyStatement, null -> {}
        else -> throw IRCodeGenError("Unknown init statement, init=$init", init.begin())
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
                if (forStatement.update is EmptyExpression) {
                    ir.branch(conditionBlock)
                } else {
                    ir.branch(loopStmtInfo.resolveUpdate(ir))
                }
            }
            val updateBB = loopStmtInfo.update()
            if (updateBB != null) {
                ir.switchLabel(updateBB)
                visitUpdate(forStatement.update)
                ir.branch(conditionBlock)
            }
            if (ir.last() !is TerminateInstruction || loopStmtInfo.exit() != null) {
                val endBlock = loopStmtInfo.resolveExit(ir)
                ir.switchLabel(endBlock)
            }
        }
    }

    override fun visit(switchStatement: SwitchStatement) = scoped {
        if (ir.last() is TerminateInstruction) {
            return@scoped
        }
        val condition = visitExpression(switchStatement.condition, true)
        val conditionBlock = ir.currentLabel()

        stmtStack.scoped(SwitchStmtInfo(condition.type().asType(), conditionBlock, arrayListOf(), arrayListOf())) { info ->
            visitStatement(switchStatement.body)
            if (ir.last() !is TerminateInstruction) {
                ir.branch(info.resolveExit(ir))
            }

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

        val irType = when (val cType = type.typeDesc.cType()) {
            is BOOL                          -> I8Type
            is CAggregateType, is CPrimitive -> mb.toIRType<NonTrivialType>(typeHolder, cType)
            else -> throw IRCodeGenError("Unknown type, type=$cType", declarator.begin())
        }

        val rvalueAdr = ir.alloc(irType)
        varStack[declarator.name()] = rvalueAdr
        return rvalueAdr
    }

    private fun zeroMemory(address: Value, type: ArrayType) {
        for (i in 0 until type.length) {
            val elementAdr = ir.gep(address, type.elementType(), I64Value.of(i))
            when (val elementType = type.elementType()) {
                is PrimitiveType -> ir.store(elementAdr, PrimitiveConstant.of(elementType, 0))
                is ArrayType -> zeroMemory(elementAdr, elementType)
                is StructType -> zeroMemory(elementAdr, elementType)
            }
        }
    }
    private fun zeroMemory(address: Value, type: StructType) {
        for (i in type.fields.indices) {
            val elementAdr = ir.gfp(address, type, I64Value.of(i))
            when (val f = type.field(i)) {
                is PrimitiveType -> ir.store(elementAdr, PrimitiveConstant.of(f, 0))
                is StructType -> zeroMemory(elementAdr, f)
                is ArrayType -> zeroMemory(elementAdr, f)
            }
        }
    }

    private fun zeroingMemory(initializerList: InitializerList, value: Value, type: CAggregateType) {
        when (type) {
            is CStructType -> {
                val irElementType = mb.toIRType<StructType>(typeHolder, type)
                for (i in initializerList.initializers.size until type.members().size) {
                    val elementAdr = ir.gfp(value, irElementType, I64Value.of(i))
                    val fieldDesc = type.fieldByIndexOrNull(i) ?:
                        throw IRCodeGenError("Unknown field, field=$i", initializerList.begin())

                    when (val f = mb.toIRLVType<NonTrivialType>(typeHolder, fieldDesc.cType())) {
                        is PrimitiveType -> ir.store(elementAdr, PrimitiveConstant.of(f, 0))
                        is ArrayType -> zeroMemory(elementAdr, f)
                        is StructType -> zeroMemory(elementAdr, f)
                    }
                }
            }
            is CArrayType -> {
                if (initializerList.resolveType(typeHolder) is CStringLiteral) {
                    return
                }

                when (val elementType = type.element().cType()) {
                    is CPrimitive -> {
                        val irElementType = mb.toIRType<PrimitiveType>(typeHolder, elementType)
                        for (i in initializerList.initializers.size until type.dimension) {
                            val elementAdr = ir.gep(value, irElementType, I64Value.of(i))
                            ir.store(elementAdr, PrimitiveConstant.of(irElementType, 0))
                        }
                    }
                    is AnyCArrayType -> {
                        val irElementType = mb.toIRType<ArrayType>(typeHolder, elementType)
                        for (i in initializerList.initializers.size until type.dimension) {
                            val elementAdr = ir.gep(value, irElementType.elementType(), I64Value.of(i))
                            when (val f = irElementType.elementType()) {
                                is PrimitiveType -> ir.store(elementAdr, PrimitiveConstant.of(f, 0))
                                is ArrayType -> zeroMemory(elementAdr, f)
                                is StructType -> zeroMemory(elementAdr, f)
                            }
                        }
                    }
                    is CStructType -> {
                        val irElementType = mb.toIRType<StructType>(typeHolder, elementType)
                        for (i in initializerList.initializers.size until type.dimension) {
                            val elementAdr = ir.gep(value, irElementType, I64Value.of(i))
                            val fieldDesc = elementType.fieldByIndexOrNull(i.toInt()) ?:
                                throw IRCodeGenError("Unknown field, field=$i", initializerList.begin())

                            when (val f = mb.toIRType<NonTrivialType>(typeHolder, fieldDesc.cType())) {
                                is PrimitiveType -> ir.store(elementAdr, PrimitiveConstant.of(f, 0))
                                is ArrayType -> zeroMemory(elementAdr, f)
                                is StructType -> zeroMemory(elementAdr, f)
                            }
                        }
                    }
                    is CUnionType -> {
                        val irElementType = mb.toIRType<StructType>(typeHolder, elementType)
                        for (i in initializerList.initializers.size until type.dimension) {
                            val elementAdr = ir.gep(value, irElementType, I64Value.of(i))
                            when (val f = irElementType.field(0)) {
                                is PrimitiveType -> ir.store(elementAdr, PrimitiveConstant.of(f, 0))
                                is ArrayType -> zeroMemory(elementAdr, f)
                                is StructType -> zeroMemory(elementAdr, f)
                            }
                        }
                    }
                    else -> throw IRCodeGenError("Unknown type, type=$elementType", initializerList.begin())
                }
            }
            is CUnionType -> {}
            else -> throw IRCodeGenError("Unknown type, type=$type", initializerList.begin())
        }
    }

    private fun visitInitializerList(initializerList: InitializerList, lvalueAdr: Value, type: CAggregateType) {
        for ((idx, init) in initializerList.initializers.withIndex()) {
            when (init) {
                is SingleInitializer -> visitSingleInitializer(init.expr, lvalueAdr, type, idx)
                is DesignationInitializer -> visitDesignationInitializer(init, lvalueAdr, type)
            }
        }
        zeroingMemory(initializerList, lvalueAdr, type)
    }

    private fun visitDesignationInitializer(designationInitializer: DesignationInitializer, value: Value, type: CAggregateType) {
        var address: Value = value
        var innerType: CType = type
        for (designator in designationInitializer.designation.designators) {
            when (designator) {
                is ArrayDesignator -> {
                    if (innerType !is CArrayType) {
                        throw IRCodeGenError("Unknown type, type=$innerType", designationInitializer.begin())
                    }

                    val fieldType = mb.toIRType<ArrayType>(typeHolder, innerType)
                    val index = designator.constEval(typeHolder)
                    innerType = innerType.element().asType()
                    address = ir.gfp(address, fieldType, I64Value.of(index))
                }
                is MemberDesignator -> {
                    if (innerType !is AnyCStructType) {
                        throw IRCodeGenError("Unknown type, type=$innerType", designationInitializer.begin())
                    }

                    val fieldType = mb.toIRType<StructType>(typeHolder, innerType)
                    val member = innerType.fieldByIndexOrNull(designator.name()) ?:
                        throw IRCodeGenError("Unknown field, field=${designator.name()}", designationInitializer.begin())

                    innerType = member.cType().asType()
                    address = ir.gfp(address, fieldType, I64Value.of(member.index))
                }
            }
        }

        if (designationInitializer.initializer is InitializerList) {
            return visitInitializerList(designationInitializer.initializer, address, innerType.asType())
        }

        val expression = visitExpression(designationInitializer.initializer, true)
        val converted = mb.toIRType<Type>(typeHolder, innerType)
        val convertedRvalue = ir.convertRVToType(expression, converted)
        ir.store(address, convertedRvalue)
    }

    private fun generateInitDeclarationExpression(rValue: Expression): Value = when (rValue) {
        is InitializerList -> {
            if (rValue.initializers.size != 1) {
                throw IRCodeGenError("Initializer list with more than one element", rValue.begin())
            }
            val initializer = rValue.initializers.first()
            if (initializer !is SingleInitializer) {
                throw IRCodeGenError("Unknown initializer, initializer=$initializer", rValue.begin())
            }
            visitInitializer(initializer)
        }
        else -> visitExpression(rValue, true)
    }

    override fun visit(initDeclarator: InitDeclarator): Value {
        val varDesc = typeHolder[initDeclarator.name()]
        if (varDesc.storageClass == StorageClass.STATIC) {
            return generateGlobalAssignmentDeclarator(initDeclarator)
        }
        val type = varDesc.typeDesc.cType()
        if (type is CPrimitive) {
            val rvalue = generateInitDeclarationExpression(initDeclarator.rvalue)
            val commonType      = mb.toIRType<Type>(typeHolder, type)
            val convertedRvalue = ir.convertRVToType(rvalue, commonType)

            val lvalueAdr = visit(initDeclarator.declarator)
            ir.store(lvalueAdr, convertedRvalue)
            return convertedRvalue
        }

        val lvalueAdr = initDeclarator.declarator.accept(this)
        when (val rvalue = initDeclarator.rvalue) {
            is InitializerList -> visitInitializerList(rvalue, lvalueAdr, varDesc.typeDesc.asType())
            is FunctionCall -> {
                val rValueType = rvalue.resolveType(typeHolder)
                if (rValueType !is AnyCStructType) {
                    throw IRCodeGenError("Unknown function type, type=$rValueType", rvalue.begin())
                }

                visitFuncCallForStructType(lvalueAdr, rValueType, rvalue)
            }
            else -> {
                val rvalueResult = visitExpression(initDeclarator.rvalue, true)
                val commonType = mb.toIRType<AggregateType>(typeHolder, type)
                ir.memcpy(lvalueAdr, rvalueResult, U64Value.of(commonType.sizeOf().toLong()))
            }
        }
        return lvalueAdr
    }

    override fun visit(arrayDeclarator: ArrayDeclarator): Value {
        TODO("Not yet implemented")
    }

    override fun visit(emptyDeclarator: EmptyDeclarator): Value {
        return UndefValue
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
        val varDesc = functionNode.declareType(functionNode.specifier, typeHolder)
        val fnType = varDesc.typeDesc.asType<CFunctionType>()

        val parameters = functionNode.functionDeclarator().params()
        val cPrototype = CFunctionPrototypeBuilder(functionNode.begin(), fnType, mb, typeHolder, varDesc.storageClass).build()

        val currentFunction = mb.createFunction(functionNode.name(), cPrototype.returnType, cPrototype.argumentTypes, cPrototype.attributes)
        val funGen = IrGenFunction(mb, typeHolder, varStack, nameGenerator, currentFunction, fnType)
        funGen.visitFun(parameters, functionNode)
    }
}