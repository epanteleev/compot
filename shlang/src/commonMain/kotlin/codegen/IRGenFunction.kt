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
import codegen.TypeConverter.convertLVToType
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
import sema.SemanticAnalysis
import sema.StatementAnalysis
import tokenizer.Position
import typedesc.*


private data class FunctionArgInfo(val args: List<Value>, val attributes: Set<FunctionAttribute>)

private class IrGenFunction(moduleBuilder: ModuleBuilder,
                    sema: SemanticAnalysis,
                    varStack: VarStack<Value>,
                    nameGenerator: NameGenerator,
                    private val functionNode: FunctionNode,
                    private val parameters: List<VarDescriptor>?,
                    private val ir: FunctionDataBuilder,
                    private val functionType: CFunctionType) :
    AbstractIRGenerator(moduleBuilder, sema, varStack, nameGenerator),
    StatementVisitor<Unit>,
    DeclaratorVisitor<Value> {
    private val stmAnalysis = StatementAnalysis.analyze(functionNode)
    private var stringToLabel = mutableMapOf<String, Label>()
    private val stmtStack = StmtStack()

    private val vaListIrType by lazy {
        mb.toIRType<StructType>(sema.typeHolder, VaStart.vaList)
    }

    private inline fun<reified T> scoped(noinline block: () -> T): T {
        return sema.typeHolder.scoped { vregStack.scoped(block) }
    }

    private fun seekOrAddLabel(name: String): Label {
        return stringToLabel[name] ?: let {
            val newLabel = ir.createLabel()
            stringToLabel[name] = newLabel
            newLabel
        }
    }

    private fun visitDeclaration(declaration: Declaration) {
        if (declaration.isTypedef) {
            return
        }
        val varDescriptors = declaration.declareVars(sema.typeHolder)
        for (varDesc in varDescriptors) {
            sema.typeHolder.addVar(varDesc)
        }
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
        val type   = compoundLiteral.accept(sema)
        val irType = mb.toIRType<AggregateType>(sema.typeHolder, type)
        val adr    = ir.alloc(irType)
        visitInitializerList(compoundLiteral.initializerList, adr, type.asType(compoundLiteral.begin()))
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
        is EmptyExpression   -> UndefValue
    }

    private fun visitBuiltInVaCopy(builtinVaCopy: BuiltinVaCopy): Value {
        val dest = visitExpression(builtinVaCopy.dest, true)
        val dstType = builtinVaCopy.dest.accept(sema)
        if (dstType !== VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $dstType", builtinVaCopy.begin())
        }
        val src = visitExpression(builtinVaCopy.src, true)
        val srcType = builtinVaCopy.src.accept(sema)
        if (srcType !== VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $srcType", builtinVaCopy.begin())
        }
        val irType = mb.toIRType<StructType>(sema.typeHolder, srcType)
        val destPtr = ir.gep(dest, irType, I64Value.of(0))
        val srcPtr = ir.gep(src, irType, I64Value.of(0))
        ir.memcpy(destPtr, srcPtr, U64Value.of(irType.sizeOf().toLong()))
        return UndefValue
    }

    private fun visitBuiltInVaEnd(builtinVaEnd: BuiltinVaEnd): Value {
        val vaListType = builtinVaEnd.vaList.accept(sema)
        if (vaListType !== VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $vaListType", builtinVaEnd.begin())
        }
        // Nothing to do
        return UndefValue
    }

    private fun visitBuiltInVaStart(builtinVaStart: BuiltinVaStart): Value {
        val vaList = visitExpression(builtinVaStart.vaList, true)
        val vaListType = builtinVaStart.vaList.accept(sema)
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
        val vaListType = builtinVaArg.assign.accept(sema)
        if (vaListType !== VaStart.vaList) {
            throw IRCodeGenError("va_list type expected, but got $vaListType", builtinVaArg.begin())
        }

        val vaList = visitExpression(builtinVaArg.assign, true)
        return when (val argCType = builtinVaArg.accept(sema)) {
            is AnyCInteger, is CPointer, is BOOL -> { //TODO add test for function pointer
                val argType = mb.toIRType<PrimitiveType>(sema.typeHolder, argCType)
                emitBuiltInVaArg(vaList, argType, VaStart.GP_OFFSET_IDX, VaStart.REG_SAVE_AREA_SIZE)
            }
            is AnyCFloat -> {
                val argType = mb.toIRType<PrimitiveType>(sema.typeHolder, argCType)
                emitBuiltInVaArg(vaList, argType, VaStart.FP_OFFSET_IDX, VaStart.FP_REG_SAVE_AREA_SIZE)
            }
            is CStructType -> { //TODO add test for union
                val irType = mb.toIRType<StructType>(sema.typeHolder, argCType)
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
                        is FloatingPointType       -> emitBuiltInVaArg(vaList, type, VaStart.FP_OFFSET_IDX, VaStart.FP_REG_SAVE_AREA_SIZE)
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
        val gpOffsetCvt = ir.convertLVToType(gpOffset, I64Type)

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
        val argPtr = ir.phi(listOf(argInReg, argInMem), PtrType, listOf(varArgInReg, varArgInStack))
        return ir.load(argType, argPtr)
    }

    private fun visitInitializer(singleInitializer: SingleInitializer): Value = when (val expr = singleInitializer.expr) {
        is InitializerListInitializer -> visitInitializer(expr.list.initializers[0] as SingleInitializer)
        is ExpressionInitializer -> visitExpression(expr.expr, true)
    }

    private fun visitSingleInitializer(singleInitializer: SingleInitializer, lvalueAdr: Value, type: CAggregateType, idx: Int) {
        when (val init = singleInitializer.expr) {
            is InitializerListInitializer -> when (type) {
                is CArrayType -> {
                    val t = type.element()
                    val irType = mb.toIRType<AggregateType>(sema.typeHolder, t.cType())
                    val fieldPtr = ir.gep(lvalueAdr, irType, I64Value.of(idx))
                    visitInitializerList(init.list, fieldPtr, t.asType())
                }
                is CStructType -> {
                    val t = type.fieldByIndexOrNull(idx) ?: throw IRCodeGenError("Field '$idx' not found", init.begin())
                    val irType = mb.toIRType<AggregateType>(sema.typeHolder, type)
                    val fieldPtr = ir.gfp(lvalueAdr, irType, I64Value.of(idx))
                    visitInitializerList(init.list, fieldPtr, t.asType())
                }
                else -> throw RuntimeException("Unknown type: type=$type")
            }
            is ExpressionInitializer -> when (val expr = init.expr) {
                is StringNode -> {
                    val element = when (type) {
                        is AnyCArrayType -> type.element().cType()
                        is AnyCStructType -> type.descriptors()[idx].cType()
                    }
                    val literal = visitStringNode(expr)
                    when (element) {
                        is CHAR, is UCHAR -> {
                            if (expr.isNotEmpty()) {
                                ir.memcpy(lvalueAdr, literal, U64Value.of(expr.length()))
                                val gep = ir.gep(lvalueAdr, I8Type, I64Value.of(expr.length()))
                                ir.store(gep, I8Value.of(0))
                            } else {
                                val gep = ir.gep(lvalueAdr, I8Type, I64Value.of(0))
                                ir.store(gep, I8Value.of(0))
                            }
                        }
                        is CPointer -> {
                            val fieldPtr = ir.gep(lvalueAdr, I64Type, I64Value.of(idx))
                            ir.store(fieldPtr, literal)
                        }
                        else -> throw IRCodeGenError("Unknown type $type", expr.begin())
                    }
                }
                else -> when (type) {
                    is AnyCArrayType -> {
                        val rvalue = visitExpression(expr, true)
                        val irType = mb.toIRType<AggregateType>(sema.typeHolder, type)

                        val field = when (val irFieldType = irType.field(idx)) {
                            is PrimitiveType -> irFieldType
                            is StructType    -> irFieldType.field(0)
                            else -> throw IRCodeGenError("Unknown type $irFieldType", expr.begin())
                        }
                        val converted = ir.convertLVToType(rvalue, field)
                        val fieldPtr = ir.gfp(lvalueAdr, irType, I64Value.of(idx))
                        ir.store(fieldPtr, converted)
                    }
                    is AnyCStructType -> {
                        val rvalue = visitExpression(expr, true)
                        val irType = mb.toIRType<AggregateType>(sema.typeHolder, type)
                        val irFieldType = irType.field(idx)
                        val converted = ir.convertLVToType(rvalue, irFieldType)
                        val fieldPtr = ir.gfp(lvalueAdr, irType, I64Value.of(idx))
                        ir.store(fieldPtr, converted)
                    }
                }
            }
        }
    }

    private fun visitCharNode(charNode: CharNode): Value {
        return ir.trunc(I32Value.of(charNode.toByte()), I8Type)
    }

    private fun castFlagTypeToI8(value: Value): Value = when (value.type()) {
        FlagType -> ir.flag2int(value, I8Type)
        else -> value
    }

    private fun generateIfElsePattern(commonType: PrimitiveType, conditional: Conditional): Value {
        val condition = makeConditionFromExpression(conditional.cond)

        val trueBB = ir.createLabel()
        val falseBB = ir.createLabel()
        val end = ir.createLabel()
        ir.branchCond(condition, trueBB, falseBB)
        ir.switchLabel(trueBB)

        val right = castFlagTypeToI8(visitExpression(conditional.eTrue, true))
        val convertedRight = ir.convertRVToType(right, commonType)

        val trueBBCurrent = ir.currentLabel()
        ir.branch(end)
        ir.switchLabel(falseBB)

        val left = castFlagTypeToI8(visitExpression(conditional.eFalse, true))
        val convertedLeft = ir.convertRVToType(left, commonType)

        val falseBBCurrent = ir.currentLabel()
        ir.branch(end)
        ir.switchLabel(end)
        return ir.phi(listOf(convertedRight, convertedLeft), commonType, listOf(trueBBCurrent, falseBBCurrent))
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
        when (val cType = conditional.accept(sema)) {
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
                val commonType = mb.toIRType<IntegerType>(sema.typeHolder, cType)
                generateSelectPattern(conditional, commonType)
            }
            is AnyCFloat, is CPointer -> {
                val commonType = mb.toIRType<PrimitiveType>(sema.typeHolder, cType)
                generateIfElsePattern(commonType, conditional)
            }
            is BOOL -> generateSelectPattern(conditional, I8Type)
            is AnyCStructType -> generateIfElsePattern(PtrType, conditional)
            else -> throw IRCodeGenError("Unknown type $cType", conditional.begin())
        }


    private fun visitArrowMemberAccess(arrowMemberAccess: ArrowMemberAccess, isRvalue: Boolean): Value {
        val struct   = visitExpression(arrowMemberAccess.primary, true)
        val cPointer = when (val ty = arrowMemberAccess.primary.accept(sema)) {
            is AnyCArrayType -> ty.asPointer()
            is CPointer      -> ty
            else -> throw IRCodeGenError("Pointer type expected, but got $ty", arrowMemberAccess.begin())
        }
        val cStructType = cPointer.dereference(arrowMemberAccess.begin(), sema.typeHolder)

        if (cStructType !is AnyCStructType) {
            throw IRCodeGenError("Struct type expected, but got '$cStructType'", arrowMemberAccess.begin())
        }
        val fieldName = arrowMemberAccess.fieldName()
        val member = cStructType.fieldByNameOrNull(fieldName) ?: let {
            throw IRCodeGenError("Field not found: $fieldName", arrowMemberAccess.begin())
        }

        val structIRType = mb.toIRType<StructType>(sema.typeHolder, cStructType)
        if (structIRType.fields().size <= member.index) {
            return ir.gep(struct, I8Type, I64Value.of(cStructType.size()))
        }

        val gep = ir.gfp(struct, structIRType, I64Value.of(member.index))
        if (!isRvalue) {
            return gep
        }

        val memberType = member.cType()
        if (memberType is CAggregateType || memberType is AnyCFunctionType) {
            return gep
        }
        val memberIRType = mb.toIRLVType<PrimitiveType>(sema.typeHolder, memberType)
        return ir.load(memberIRType, gep)
    }

    private fun visitMemberAccess(memberAccess: MemberAccess, isRvalue: Boolean): Value {
        val struct = visitExpression(memberAccess.primary, false) //TODO isRvalue???
        val structType = memberAccess.primary.accept(sema)
        if (structType !is AnyCStructType) {
            throw IRCodeGenError("Struct type expected, but got '$structType'", memberAccess.begin())
        }

        val fieldName = memberAccess.memberName()
        val member = structType.fieldByNameOrNull(fieldName) ?:
            throw IRCodeGenError("Field not found: '$fieldName'", memberAccess.begin())

        val structIRType = mb.toIRType<StructType>(sema.typeHolder, structType)
        if (structIRType.fields().size <= member.index) {
            return ir.gep(struct, I8Type, I64Value.of(structType.size()))
        }

        val gep = ir.gfp(struct, structIRType, I64Value.of(member.index))
        if (!isRvalue) {
            return gep
        }

        val memberType = member.cType()
        if (memberType is CAggregateType || memberType is AnyCFunctionType) {
            return gep
        }

        val memberIRType = mb.toIRLVType<PrimitiveType>(sema.typeHolder, memberType)
        return ir.load(memberIRType, gep)
    }

    private fun visitSizeOf(sizeOf: SizeOf): Value = I64Value.of(sizeOf.constEval(sema))

    private fun visitStringNode(stringNode: StringNode): Value {
        val stringLiteral = StringLiteralGlobalConstant(createStringLiteralName(), ArrayType(I8Type, stringNode.length()), stringNode.data())
        return mb.addConstant(stringLiteral)
    }

    private fun visitArrayAccess(arrayAccess: ArrayAccess, isRvalue: Boolean): Value {
        val index = visitExpression(arrayAccess.expr, true)
        val convertedIndex = ir.convertLVToType(index, I64Type)

        val array = visitExpression(arrayAccess.primary, true)
        val arrayType = arrayAccess.accept(sema)
        val elementType = mb.toIRLVType<NonTrivialType>(sema.typeHolder, arrayType)

        val adr = when (val primaryType = arrayAccess.primary.accept(sema)) {
            is AnyCArrayType, is CPointer -> ir.gep(array, elementType, convertedIndex)
            is CPrimitive -> {
                val arrValue = ir.convertLVToType(array, I64Type)
                val mul = ir.mul(arrValue, I64Value.of(elementType.sizeOf()))
                val add = ir.add(mul, convertedIndex)
                ir.convertLVToType(add, PtrType)
            }
            else -> throw IRCodeGenError("Unknown type, type=$primaryType in array access", arrayAccess.begin())
        }

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
            return ir.convertLVToType(expr, cvt)
        }

        if (!function.attributes.contains(VarArgAttribute)) {
            throw IRCodeGenError("Too many arguments in function call '${function.shortDescription()}'", Position.UNKNOWN) //TODO correct position
        }
        return when (expr.type()) {
            F32Type -> ir.convertLVToType(expr, F64Type)
            I8Type, I16Type, FlagType -> ir.convertLVToType(expr, I32Type)
            U8Type, U16Type -> ir.convertLVToType(expr, U32Type)
            else -> expr
        }
    }

    private fun convertFunctionArgs(function: AnyFunctionPrototype, returnType: CType, args: List<Expression>): FunctionArgInfo {
        val convertedArgs = mutableListOf<Value>()
        val attributes = hashSetOf<FunctionAttribute>()

        var offset = evaluateFirstArgIdx(returnType)
        for ((idx, argValue) in args.withIndex()) {
            val expr = visitExpression(argValue, true)
            when (val argCType = argValue.accept(sema)) {
                is CPrimitive, is AnyCFunctionType, is CUncompletedArrayType, is CStringLiteral -> {
                    val convertedArg = convertArg(function, idx + offset, expr)
                    convertedArgs.add(convertedArg)
                }
                is CArrayType -> {
                    val type = mb.toIRType<ArrayType>(sema.typeHolder, argCType)
                    val convertedArg = ir.gep(expr, type.elementType(), I64Value.of(0))
                    convertedArgs.add(convertedArg)
                }
                is AnyCStructType -> {
                    if (argCType === VaStart.vaList) {
                        convertedArgs.add(expr)
                        continue
                    }
                    if (!argCType.isSmall()) {
                        val irType = mb.toIRType<StructType>(sema.typeHolder, argCType)
                        attributes.add(ByValue(idx + offset, irType))
                    }
                    val argValues = ir.loadCoerceArguments(argCType, expr)
                    convertedArgs.addAll(argValues)
                    offset += argValues.size - 1
                }
            }
        }
        return FunctionArgInfo(convertedArgs, attributes)
    }

    private fun visitCast(cast: Cast): Value {
        val value = visitExpression(cast.cast, true)
        return when (val castToType = cast.accept(sema)) {
            is VOID -> value
            is BOOL -> ir.convertLVToType(value, FlagType)
            is CPointer  -> {
                val baseAddr = when (val fromType = cast.cast.accept(sema)) {
                    is CArrayType -> {
                        val irType = mb.toIRType<ArrayType>(sema.typeHolder, fromType)
                        ir.gep(value, irType.elementType(), I64Value.of(0))
                    }
                    is AnyCStructType -> {
                        val irType = mb.toIRType<StructType>(sema.typeHolder, fromType)
                        ir.gfp(value, irType, I64Value.of(0))
                    }
                    is CPrimitive,
                    is AnyCFunctionType,
                    is CStringLiteral,
                    is CUncompletedArrayType -> value
                }
                ir.convertLVToType(baseAddr, PtrType)
            }
            is CPrimitive -> {
                val toType = mb.toIRType<PrimitiveType>(sema.typeHolder, castToType)
                ir.convertLVToType(value, toType)
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
                val structType = mb.toIRType<StructType>(sema.typeHolder, returnType)
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
        val functionType = sema.functionType(funcPointerCall)
        val loadedFunctionPtr = visitExpression(funcPointerCall.primary, true)

        val cPrototype = CFunctionPrototypeBuilder(funcPointerCall.begin(), functionType, mb, sema.typeHolder, StorageClass.AUTO).build()

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
                val retType = mb.toIRType<NonTrivialType>(sema.typeHolder, functionReturnType)
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

        return when (val functionType = functionCall.accept(sema)) {
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
                val retType = mb.toIRType<NonTrivialType>(sema.typeHolder, functionType)
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
        val functionType = sema.functionType(funcPointerCall)
        val loadedFunctionPtr = visitExpression(funcPointerCall.primary, true)

        val cPrototype = CFunctionPrototypeBuilder(funcPointerCall.begin(), functionType, mb, sema.typeHolder, StorageClass.AUTO).build()

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

    private fun evaluateAddress(expr: Expression, addressCType: CPointer): Value {
        val rvalue = visitExpression(expr, true)
        return when (val exprType = expr.accept(sema)) {
            is AnyCArrayType, is CPointer -> ir.convertLVToType(rvalue, I64Type)
            is CPrimitive -> {
                val convertedLValue = ir.convertLVToType(rvalue, I64Type)
                val dereferenced = addressCType.dereference(expr.begin(), sema.typeHolder)
                ir.mul(convertedLValue, I64Value.of(dereferenced.size()))
            }

            else -> throw IRCodeGenError("Primitive type expected, but got $exprType", expr.begin())
        }
    }

    private fun makeSubBinary(binOp: BinaryOp): Value = when (val cType = binOp.accept(sema)) {
        is CPointer -> {
            val lAddress = evaluateAddress(binOp.left, cType)
            val rAddress = evaluateAddress(binOp.right, cType)
            val result = ir.sub(lAddress, rAddress)
            val rType = binOp.right.accept(sema)
            val lType = binOp.left.accept(sema)
            val divided = if (rType is CPointer && lType is CPointer) {
                val vType = cType.dereference(binOp.begin(), sema.typeHolder)
                    .asType<CompletedType>(binOp.begin())
                divide(result, I64Value.of(vType.size()))
            } else {
                result
            }

            ir.convertRVToType(divided, PtrType)
        }
        is AnyCFloat -> { //TODO code duplication with makeAlgebraicBinary
            val commonType = mb.toIRType<FloatingPointType>(sema.typeHolder, cType)
            val left = visitExpression(binOp.left, true)
            val leftConverted = ir.convertRVToType(left, commonType)

            val right = visitExpression(binOp.right, true)
            val rightConverted = ir.convertRVToType(right, commonType)

            ir.sub(leftConverted, rightConverted)
        }
        is AnyCInteger, is CEnumType -> { //TODO code duplication with makeAlgebraicBinary
            val cvtType = when (val commonType = mb.toIRType<IntegerType>(sema.typeHolder, cType)) {
                is SignedIntType   -> if (commonType.sizeOf() < WORD_SIZE) I32Type else commonType
                is UnsignedIntType -> if (commonType.sizeOf() < WORD_SIZE) U32Type else commonType
            }
            val left = visitExpression(binOp.left, true)
            val leftConverted = ir.convertLVToType(left, cvtType)

            val right = visitExpression(binOp.right, true)
            val rightConverted = ir.convertLVToType(right, cvtType)

            ir.sub(leftConverted, rightConverted)
        }
        else -> throw IRCodeGenError("Unexpected type: $cType", binOp.begin())
    }

    private fun makeAlgebraicBinary(binOp: BinaryOp, op: (a: Value, b: Value) -> Value): Value = when (val cType = binOp.accept(sema)) {
        is CPointer -> {
            val lAddress = evaluateAddress(binOp.left, cType)
            val rAddress = evaluateAddress(binOp.right, cType)
            val result = op(lAddress, rAddress)
            ir.convertRVToType(result, PtrType)
        }
        is AnyCFloat -> {
            val commonType = mb.toIRType<FloatingPointType>(sema.typeHolder, cType)
            val left = visitExpression(binOp.left, true)
            val leftConverted = ir.convertRVToType(left, commonType)

            val right = visitExpression(binOp.right, true)
            val rightConverted = ir.convertRVToType(right, commonType)

            op(leftConverted, rightConverted)
        }
        is AnyCInteger, is CEnumType -> {
            val cvtType = when (val commonType = mb.toIRType<IntegerType>(sema.typeHolder, cType)) {
                is SignedIntType   -> if (commonType.sizeOf() < WORD_SIZE) I32Type else commonType
                is UnsignedIntType -> if (commonType.sizeOf() < WORD_SIZE) U32Type else commonType
            }
            val left = visitExpression(binOp.left, true)
            val leftConverted = ir.convertLVToType(left, cvtType)

            val right = visitExpression(binOp.right, true)
            val rightConverted = ir.convertLVToType(right, cvtType)

            op(leftConverted, rightConverted)
        }
        is BOOL -> {
            val left = visitExpression(binOp.left, true)
            val leftConverted = ir.convertLVToType(left, I32Type)

            val right = visitExpression(binOp.right, true)
            val rightConverted = ir.convertLVToType(right, I32Type)

            op(leftConverted, rightConverted)
        }
        else -> throw IRCodeGenError("Unexpected type: $cType", binOp.begin())
    }

    private fun makeAlgebraicBinaryWithAssignment(binOp: BinaryOp, op: (a: Value, b: Value) -> Value): Value {
        when (val commonType = mb.toIRType<NonTrivialType>(sema.typeHolder, binOp.accept(sema))) {
            is PtrType -> {
                val rvalue     = visitExpression(binOp.right, true)
                val rValueType = binOp.right.accept(sema)
                if (rValueType !is CPrimitive) {
                    throw IRCodeGenError("Primitive type expected, but got $rValueType", binOp.begin())
                }
                val convertedRValue = ir.convertLVToType(rvalue, I64Type)

                val lvalueAddress = visitExpression(binOp.left, false)
                val lValueType    = binOp.accept(sema)
                val lvalue        = ir.load(PtrType, lvalueAddress)
                val ptr2intLValue = ir.ptr2int(lvalue, I64Type)

                if (lValueType !is CPointer) {
                    throw IRCodeGenError("Pointer type expected, but got $lValueType", binOp.begin())
                }
                val convertedLValue = ir.convertLVToType(ptr2intLValue, I64Type)

                val dereferenced = lValueType.dereference(binOp.begin(), sema.typeHolder)
                val mul = ir.mul(convertedRValue, I64Value.of(dereferenced.size()))

                val result = op(convertedLValue, mul)
                val res = ir.convertLVToType(result, commonType)
                ir.store(lvalueAddress, res)
                return res
            }
            is FloatingPointType -> {
                val right = visitExpression(binOp.right, true)
                val leftType = binOp.left.accept(sema)
                val leftIrType = mb.toIRType<PrimitiveType>(sema.typeHolder, leftType)
                val rightConverted = ir.convertLVToType(right, leftIrType)

                val left = visitExpression(binOp.left, false)
                val loadedLeft = ir.load(leftIrType, left)

                val sum = op(loadedLeft, rightConverted)
                ir.store(left, sum)
                return sum
            }
            is IntegerType -> {
                val right = visitExpression(binOp.right, true)
                val leftType = binOp.left.accept(sema)
                val originalIrType = mb.toIRType<IntegerType>(sema.typeHolder, leftType)
                val leftIrType = when (originalIrType) {
                    is SignedIntType -> if (originalIrType.sizeOf() < WORD_SIZE) I32Type else originalIrType
                    is UnsignedIntType -> if (originalIrType.sizeOf() < WORD_SIZE) U32Type else originalIrType
                }
                val rightConverted = ir.convertLVToType(right, leftIrType)

                val left = visitExpression(binOp.left, false)
                val loadedLeft = ir.load(originalIrType, left)
                val cvtLft = ir.convertLVToType(loadedLeft, leftIrType)

                val sum = op(cvtLft, rightConverted)
                val sumCvt = ir.convertLVToType(sum, originalIrType)
                ir.store(left, sumCvt)
                return sum
            }
            else -> throw RuntimeException("Unknown type: type=$commonType")
        }
    }

    private fun makeCompareBinary(binOp: BinaryOp, predicate: (PrimitiveType) -> AnyPredicateType, isRvalue: Boolean): Value {
        val commonType = mb.toIRType<Type>(sema.typeHolder, binOp.accept(sema))
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
        return ir.convertLVToType(cmp, I8Type)
    }

    private fun visitAssignBinary(binop: BinaryOp): Value {
        val leftType = binop.left.accept(sema)
        if (leftType is AnyCStructType && binop.right is FunctionCall) {
            val lvalueAdr = visitExpression(binop.left, false)
            visitFuncCallForStructType(lvalueAdr, leftType, binop.right)
            return lvalueAdr
        }

        val right = visitExpression(binop.right, true)
        if (leftType !is AnyCStructType) {
            val leftIrType = mb.toIRType<Type>(sema.typeHolder, leftType)
            val rightCvt = ir.convertRVToType(right, leftIrType)

            val left = visitExpression(binop.left, false)
            ir.store(left, rightCvt)
            return rightCvt
        }
        val rightType = binop.right.accept(sema)
        val left = visitExpression(binop.left, true)
        ir.memcpy(left, right, U64Value.of(rightType.size().toLong()))
        return right
    }

    private fun cvtToI8(right: Value): Value = when (val ty = right.type()) {
        is FlagType -> ir.convertLVToType(right, I8Type)
        is IntegerType -> {
            val cmp = ir.icmp(right, IntPredicate.Ne, IntegerConstant.of(ty, 0))
            ir.convertLVToType(cmp, I8Type)
        }
        is PtrType -> {
            val cmp = ir.icmp(right, IntPredicate.Ne, NullValue)
            ir.convertLVToType(cmp, I8Type)
        }
        is FloatingPointType -> {
            val cmp = ir.fcmp(right, FloatPredicate.Oeq, FloatingPointConstant.of(ty, 0.0))
            ir.convertLVToType(cmp, I8Type)
        }
        else -> throw RuntimeException("Unknown type: type=$ty")
    }

    private fun visitBinary(binOp: BinaryOp, isRvalue: Boolean): Value = when (binOp.opType) { // TODO can compile incorrect code
        BinaryOpType.ADD -> makeAlgebraicBinary(binOp, ir::add)
        BinaryOpType.SUB -> makeSubBinary(binOp)
        BinaryOpType.ASSIGN -> visitAssignBinary(binOp)
        BinaryOpType.ADD_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::add)
        BinaryOpType.DIV_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ::divide)
        BinaryOpType.MUL_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::mul)
        BinaryOpType.BIT_OR -> makeAlgebraicBinary(binOp, ir::or)
        BinaryOpType.MUL -> makeAlgebraicBinary(binOp, ir::mul)
        BinaryOpType.NE -> makeCompareBinary(binOp, ::ne, isRvalue)
        BinaryOpType.GT -> makeCompareBinary(binOp, ::gt, isRvalue)
        BinaryOpType.LT -> makeCompareBinary(binOp, ::lt, isRvalue)
        BinaryOpType.LE -> makeCompareBinary(binOp, ::le, isRvalue)
        BinaryOpType.AND -> {
            val left = visitExpression(binOp.left, true)
            val convertedLeft = ir.convertLVToType(left, FlagType)

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
            ir.phi(listOf(I8Value.of(0), convertedRight), I8Type, listOf(initialBB, current))
        }
        BinaryOpType.OR -> {
            val left = visitExpression(binOp.left, true)
            val convertedLeft = ir.convertLVToType(left, FlagType)

            val bb = ir.createLabel()
            val end = ir.createLabel()
            val initialBB = ir.currentLabel()
            ir.branchCond(convertedLeft, end, bb)
            ir.switchLabel(bb)

            val right = visitExpression(binOp.right, true)
            val convertedRight = cvtToI8(right)
            val current = ir.currentLabel()

            ir.branch(end)
            ir.switchLabel(end)
            ir.phi(listOf(I8Value.of(1), convertedRight), I8Type, listOf(initialBB, current))
        }
        BinaryOpType.SHR_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::shr)
        BinaryOpType.SHL_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::shl)
        BinaryOpType.BIT_XOR_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::xor)
        BinaryOpType.BIT_OR_ASSIGN -> makeAlgebraicBinaryWithAssignment(binOp, ir::or)
        BinaryOpType.GE  -> makeCompareBinary(binOp, ::ge, isRvalue)
        BinaryOpType.EQ  -> makeCompareBinary(binOp, ::eq, isRvalue)
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
            visitExpression(binOp.right, true)
        }
    }

    private fun divide(a: Value, b: Value): Value = when (val type = a.type()) {
        is IntegerType -> ir.tupleDiv(a, b).quotient
        is FloatingPointType -> ir.div(a, b)
        else -> throw RuntimeException("Unknown type: type=$type")
    }

    private fun rem(a: Value, b: Value): Value = when (val type = a.type()) {
        is IntegerType -> ir.tupleDiv(a, b).remainder
        else -> throw RuntimeException("Unknown type: type=$type")
    }

    private fun visitIncOrDec(unaryOp: UnaryOp, op: (a: Value, b: Value) -> LocalValue): Value {
        assertion(unaryOp.opType == PostfixUnaryOpType.INC || unaryOp.opType == PostfixUnaryOpType.DEC) {
            "Unknown operation, op=${unaryOp.opType}"
        }

        val ctype = unaryOp.accept(sema)
        val addr = visitExpression(unaryOp.primary, false)
        val type = mb.toIRType<PrimitiveType>(sema.typeHolder, ctype)
        val loaded = ir.load(type, addr)
        when (ctype) {
            is CPointer -> {
                val converted = ir.convertLVToType(loaded, I64Type)
                val dereferenced = ctype.dereference(unaryOp.begin(), sema.typeHolder)
                val inc = op(converted, I64Value.of(dereferenced.size()))
                ir.store(addr, ir.convertLVToType(inc, type))
            }
            is CPrimitive -> {
                val inc = op(loaded, PrimitiveConstant.of(loaded.type(), 1))
                ir.store(addr, ir.convertLVToType(inc, type))
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
        when (val cType = unaryOp.accept(sema)) {
            is CPointer -> {
                val loaded    = ir.load(PtrType, address)
                val converted = ir.convertLVToType(loaded, I64Type)
                val dereferenced = cType.dereference(unaryOp.begin(), sema.typeHolder)
                val inc       = op(converted, I64Value.of(dereferenced.size()))
                val incPtr    = ir.convertLVToType(inc, PtrType)
                ir.store(address, incPtr)
                return incPtr
            }
            is CPrimitive -> {
                val type   = mb.toIRType<PrimitiveType>(sema.typeHolder, cType)
                val loaded = ir.load(type, address)
                val inc    = op(loaded, PrimitiveConstant.of(loaded.type(), 1))
                ir.store(address, ir.convertLVToType(inc, type))
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
                val type = unaryOp.accept(sema)
                if (type is CAggregateType || type is AnyCFunctionType) {
                    return addr
                }

                val loadedType = mb.toIRLVType<PrimitiveType>(sema.typeHolder, type)
                val cvtAddr = when (unaryOp.primary.accept(sema)) {
                    is AnyCArrayType -> ir.gep(addr, loadedType, I64Value.of(0))
                    else -> addr
                }

                ir.load(loadedType, cvtAddr)
            }
            PostfixUnaryOpType.INC -> visitIncOrDec(unaryOp, ir::add)
            PostfixUnaryOpType.DEC -> visitIncOrDec(unaryOp, ir::sub)
            PrefixUnaryOpType.INC  -> visitPrefixIncOrDec(unaryOp, ir::add)
            PrefixUnaryOpType.DEC  -> visitPrefixIncOrDec(unaryOp, ir::sub)
            PrefixUnaryOpType.NEG  -> {
                val value = visitExpression(unaryOp.primary, true)
                val type = unaryOp.accept(sema)
                val valueType = mb.toIRType<NonTrivialType>(sema.typeHolder, type)
                val converted = ir.convertLVToType(value, valueType)
                ir.neg(converted)
            }
            PrefixUnaryOpType.NOT -> {
                val value = visitExpression(unaryOp.primary, true)
                val type = unaryOp.accept(sema)
                val commonType = mb.toIRType<Type>(sema.typeHolder, type)
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
                val type = unaryOp.accept(sema)
                val commonType = mb.toIRType<NonTrivialType>(sema.typeHolder, type)
                val converted = ir.convertLVToType(value, commonType)
                ir.not(converted)
            }
            PrefixUnaryOpType.PLUS -> visitExpression(unaryOp.primary, isRvalue)
        }
    }

    private fun visitNumNode(numNode: NumNode): Constant = when (numNode.number.type) {
        INT -> I32Value.of(numNode.number.number().toInt())
        LONG -> I64Value.of(numNode.number.number().toLong())
        ULONG -> U64Value.of(numNode.number.number().toLong())
        DOUBLE -> F64Value(numNode.number.number().toDouble())
        else -> throw IRCodeGenError("Unknown number type, num=${numNode.number.str()}", numNode.begin())
    }

    private fun isArgumentVariable(varNode: VarNode): Boolean {
        if (parameters == null) {
            return false
        }
        val name = varNode.name()
        return parameters.find { it.name == name } != null
    }

    private fun getVariableAddress(varNode: VarNode, rvalueAddr: Value, isRvalue: Boolean): Value {
        if (!isRvalue) {
            return rvalueAddr
        }
        when (val type = varNode.accept(sema)) {
            is AnyCStructType, is AnyCFunctionType -> {
                return rvalueAddr
            }
            is AnyCArrayType -> {
                return if (!isArgumentVariable(varNode)) {
                    rvalueAddr
                } else {
                    ir.load(PtrType, rvalueAddr)
                }
            }
            is CPrimitive -> {
                val converted = mb.toIRLVType<PrimitiveType>(sema.typeHolder, type)
                return ir.load(converted, rvalueAddr)
            }
        }
    }

    private fun visitVarNode(varNode: VarNode, isRvalue: Boolean): Value {
        val name = varNode.name()
        val rvalueAttr = vregStack[name]
        if (rvalueAttr != null) {
            return getVariableAddress(varNode, rvalueAttr, isRvalue)
        }
        val global = mb.findFunction(name)
        if (global != null) {
            return global
        }

        val enumValue = sema.typeHolder.findEnumByEnumerator(name)
        if (enumValue != null) {
            return I32Value.of(enumValue)
        }

        throw IRCodeGenError("Variable '$name' not found", varNode.begin())
    }

    private fun visitPrimitiveParameter(param: VarDescriptor, arg: ArgumentValue) {
        when (val cType = param.cType()) {
            is AnyCArrayType -> {
                val rvalueAdr = ir.alloc(PtrType)
                ir.store(rvalueAdr, ir.convertLVToType(arg, PtrType))
                vregStack[param.name] = rvalueAdr
            }
            is CPrimitive, is AnyCFunctionType -> {
                val irType = mb.toIRLVType<PrimitiveType>(sema.typeHolder, cType)
                val rvalueAdr = ir.alloc(irType)
                ir.store(rvalueAdr, ir.convertLVToType(arg, irType))
                vregStack[param.name] = rvalueAdr
            }
            is AnyCStructType -> {
                if (cType.isSmall()) {
                    throw RuntimeException("internal error")
                }

                vregStack[param.name] = arg
            }
        }
    }

    private fun visitParameter(param: String, cType: AnyCStructType, args: List<ArgumentValue>) {
        if (!cType.isSmall()) {
            throw RuntimeException("internal error")
        }

        val irType    = mb.toIRType<NonTrivialType>(sema.typeHolder, cType)
        val rvalueAdr = ir.alloc(irType)
        ir.storeCoerceArguments(cType, rvalueAdr, args)
        vregStack[param] = rvalueAdr
    }

    private fun visitParameters(parameters: List<VarDescriptor>, arguments: List<ArgumentValue>, retType: CType, where: Position) {
        var argumentIdx = evaluateFirstArgIdx(retType)
        for (currentArg in parameters.indices) {
            val param = parameters[currentArg]
            when (val cType = param.cType()) {
                is CPrimitive, is AnyCArrayType, is AnyCFunctionType -> visitPrimitiveParameter(param, arguments[argumentIdx])
                is AnyCStructType -> {
                    if (!cType.isSmall()) {
                        visitPrimitiveParameter(param, arguments[argumentIdx])
                        argumentIdx++
                        continue
                    }

                    val types = CallConvention.coerceArgumentTypes(cType) ?: throw RuntimeException("internal error")
                    val args = mutableListOf<ArgumentValue>()
                    for (i in types.indices) {
                        args.add(arguments[argumentIdx + i])
                    }
                    argumentIdx += types.size - 1
                    visitParameter(param.name, cType, args)
                }
            }
            argumentIdx++
        }
    }

    private fun visitReturnType(fnStmt: FunctionStmtInfo, retCType: TypeDesc, args: List<ArgumentValue>, where: Position) {
        val exitBlock = fnStmt.resolveExit(ir)
        when (val cType = retCType.asType<CompletedType>()) {
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
            is CPrimitive, is AnyCFunctionType -> {
                val retType = mb.toIRLVType<PrimitiveType>(sema.typeHolder, retCType.cType())
                val returnValueAdr = fnStmt.resolveReturnValueAdr { ir.alloc(retType) }
                ir.switchLabel(exitBlock)
                val ret = ir.load(retType, returnValueAdr)
                ir.ret(retType, arrayOf(ret))
            }
            is AnyCStructType -> when (val irRetType = ir.prototype().returnType()) {
                is PrimitiveType -> {
                    val returnValueAdr = fnStmt.resolveReturnValueAdr {
                        ir.alloc(mb.toIRType<StructType>(sema.typeHolder, cType))
                    }
                    ir.switchLabel(exitBlock)

                    ir.ret(irRetType, arrayOf(returnValueAdr))
                }
                is TupleType -> {
                    val returnValueAdr = fnStmt.resolveReturnValueAdr {
                        ir.alloc(mb.toIRType<StructType>(sema.typeHolder, cType))
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
            is AnyCArrayType -> throw IRCodeGenError("Array type not supported in return type", where)
        }
    }

    private fun initializeVarArgs(fnType: CFunctionType, fnStmt: FunctionStmtInfo) {
        if (!fnType.variadic()) {
            return
        }
        val vaInitType = mb.toIRType<StructType>(sema.typeHolder, VaInit.vaInit)
        val vaInitInstance = ir.alloc(vaInitType)
        fnStmt.vaInit = vaInitInstance
        val cont = ir.createLabel()
        ir.intrinsic(arrayListOf(vaInitInstance), VaInit(fnType.args().first().cType()), cont)
        ir.switchLabel(cont)
    }

    private fun visitParameters(functionNode: FunctionNode) {
        if (parameters == null) {
            return
        }

        for (param in parameters) {
            sema.typeHolder.addVar(param)
        }

        val retType = functionNode.cFunctionType().retType().cType()
        visitParameters(parameters, ir.arguments(), retType, functionNode.begin())
    }

    fun visitFun(): Value = scoped {
        stmtStack.scoped(FunctionStmtInfo()) { stmt ->
            visitParameters(functionNode)
            visitReturnType(stmt, functionType.retType(), ir.arguments(), functionNode.begin())

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
        if (stmAnalysis.isUnreachable(exprStatement)) {
            if (ir.last() !is TerminateInstruction) {
                throw RuntimeException("Here: ${exprStatement.begin()}")
            }
            return
        }

        visitExpression(exprStatement.expr, true)
    }

    override fun visit(labeledStatement: LabeledStatement) {
        if (stmAnalysis.isUnreachable(labeledStatement) && labeledStatement.gotos().isEmpty()) {
            if (ir.last() !is TerminateInstruction) {
                throw RuntimeException("Here: ${labeledStatement.begin()}")
            }
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
        if (stmAnalysis.isUnreachable(gotoStatement)) {
            if (ir.last() !is TerminateInstruction) {
                throw RuntimeException("Here: ${gotoStatement.begin()}")
            }
            return
        }

        if (gotoStatement.label() == null) {
            throw IRCodeGenError("Goto statement outside of labeled statement", gotoStatement.begin())
        }

        val label = seekOrAddLabel(gotoStatement.id.str())
        ir.branch(label)
    }

    override fun visit(continueStatement: ContinueStatement) {
        if (stmAnalysis.isUnreachable(continueStatement)) {
            if (ir.last() !is TerminateInstruction) {
                throw RuntimeException("Here: ${continueStatement.begin()}")
            }
            return
        }

        when (val loopInfo = stmtStack.topLoop()) {
            is ForLoopStmtInfo -> ir.branch(loopInfo.resolveUpdate(ir)) //TODO bug???
            is LoopStmtInfo    -> ir.branch(loopInfo.resolveCondition(ir))
            null               -> throw IRCodeGenError("Continue statement outside of loop", continueStatement.begin())
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

    override fun visit(defaultStatement: DefaultStatement) {
        val switchInfo = stmtStack.top() as SwitchStmtInfo
        val default = switchInfo.resolveDefault(ir)

        if (ir.last() !is TerminateInstruction && ir.currentLabel() != switchInfo.condBlock) {
            ir.branch(default)
        }

        ir.switchLabel(default)
        visitStatement(defaultStatement.stmt)
        switchInfo.markVisited(defaultStatement)
    }

    override fun visit(caseStatement: CaseStatement) {
        val switchInfo = stmtStack.top() as SwitchStmtInfo

        val ctx = CommonConstEvalContext<Int>(sema)
        val constant = ConstEvalExpression.eval(caseStatement.constExpression, TryConstEvalExpressionInt(ctx))
            ?: throw IRCodeGenError("Case statement with non-constant expression: ${LineAgnosticAstPrinter.print(caseStatement.constExpression)}", caseStatement.begin())

        val caseValueConverted = IntegerConstant.of(switchInfo.conditionType.asType(), constant)
        val caseBlock = ir.createLabel()
        if (ir.last() !is TerminateInstruction) {
            if (switchInfo.isFallThrough(ir.currentLabel())) {
                // fall through
                ir.branch(caseBlock)
            } else if (ir.currentLabel() != switchInfo.condBlock) {
                // fall through
                ir.branch(caseBlock)
            }
        }

        switchInfo.table.add(caseBlock)
        switchInfo.values.add(caseValueConverted)
        switchInfo.markVisited(caseStatement)

        ir.switchLabel(caseBlock)
        visitStatement(caseStatement.stmt)
    }

    override fun visit(returnStatement: ReturnStatement) {
        if (stmAnalysis.isUnreachable(returnStatement)) {
            if (ir.last() !is TerminateInstruction) {
                throw RuntimeException("Here: ${returnStatement.begin()}")
            }
            return
        }

        val fnStmt = stmtStack.root()
        val expr = returnStatement.expr
        if (expr is EmptyExpression) {
            ir.branch(fnStmt.resolveExit(ir))
            return
        }

        val value = visitExpression(expr, true)
        when (val type = returnStatement.expr.accept(sema)) {
            is CPrimitive, is CStringLiteral, is AnyCFunctionType -> when (functionType.retType().cType()) {
                is BOOL -> {
                    val returnType = ir.prototype().returnType().asType<PrimitiveType>()
                    val returnValue = ir.convertLVToType(value, FlagType)
                    val cvt = ir.convertLVToType(returnValue, returnType)
                    ir.store(fnStmt.returnValueAdr(), cvt)
                }
                is CPrimitive -> {
                    val returnType = ir.prototype().returnType().asType<PrimitiveType>()
                    val returnValue = ir.convertLVToType(value, returnType)
                    ir.store(fnStmt.returnValueAdr(), returnValue)
                }
                else -> throw RuntimeException("internal error")
            }
            is AnyCStructType -> {
                ir.memcpy(fnStmt.returnValueAdr(), value, U64Value.of(type.size().toLong()))
            }
            is AnyCArrayType -> {
                val returnType = ir.prototype().returnType().asType<PtrType>()
                val returnValue = ir.convertLVToType(value, returnType)
                ir.store(fnStmt.returnValueAdr(), returnValue)
            }
        }
        ir.branch(fnStmt.resolveExit(ir))
    }

    override fun visit(compoundStatement: CompoundStatement) = scoped {
        if (stmAnalysis.isUnreachable(compoundStatement)) {
            if (ir.last() !is TerminateInstruction) {
                throw RuntimeException("Here: ${compoundStatement.begin()}")
            }
            return@scoped
        }
        for (node in compoundStatement.statements) {
            when (node) {
                is CompoundStmtDeclaration -> visitDeclaration(node.declaration)
                is CompoundStmtStatement -> visitStatement(node.statement)
            }
        }
    }

    override fun visit(ifElseStatement: IfElseStatement) = scoped {
        if (stmAnalysis.isUnreachable(ifElseStatement)) {
            if (ir.last() !is TerminateInstruction) {
                throw RuntimeException("Here: ${ifElseStatement.begin()}")
            }
            return@scoped
        }
        val condition = makeConditionFromExpression(ifElseStatement.condition)
        val thenBlock = ir.createLabel()

        val elseBlock = ir.createLabel()
        ir.branchCond(condition, thenBlock, elseBlock)
        // then
        ir.switchLabel(thenBlock)
        visitStatement(ifElseStatement.then)
        val endBlock = if (ir.last() !is TerminateInstruction) {
            val endBlock = ir.createLabel()
            ir.branch(endBlock)
            endBlock
        } else {
            null
        }

        // else
        ir.switchLabel(elseBlock)
        visitStatement(ifElseStatement.elseNode)

        if (ir.last() !is TerminateInstruction) {
            val newEndBlock = endBlock ?: ir.createLabel()
            ir.branch(newEndBlock)
            ir.switchLabel(newEndBlock)
        } else if (endBlock != null) {
            ir.switchLabel(endBlock)
        }
    }

    override fun visit(ifStatement: IfStatement) = scoped {
        if (stmAnalysis.isUnreachable(ifStatement)) {
            if (ir.last() !is TerminateInstruction) {
                throw RuntimeException("Here: ${ifStatement.begin()}")
            }
            return@scoped
        }
        val condition = makeConditionFromExpression(ifStatement.condition)
        val thenBlock = ir.createLabel()

        val endBlock = ir.createLabel()
        ir.branchCond(condition, thenBlock, endBlock)
        ir.switchLabel(thenBlock)
        visitStatement(ifStatement.then)
        if (ir.last() !is TerminateInstruction) {
            ir.branch(endBlock)
        }
        ir.switchLabel(endBlock)
    }

    override fun visit(doWhileStatement: DoWhileStatement) = scoped {
        if (stmAnalysis.isUnreachable(doWhileStatement)) {
            if (ir.last() !is TerminateInstruction) {
                throw RuntimeException("Here: ${doWhileStatement.begin()}")
            }
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
        if (stmAnalysis.isUnreachable(whileStatement)) {
            if (ir.last() !is TerminateInstruction) {
                throw RuntimeException("Here: ${whileStatement.begin()}")
            }
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

    private fun visitInit(init: ForInit) = when (init) {
        is ForInitDeclaration -> visitDeclaration(init.declaration)
        is ForInitExpression -> visit(init.expression)
        is ForInitEmpty -> {}
    }

    private fun visitUpdate(update: Expression) {
        if (update is EmptyExpression) {
            return
        }

        visitExpression(update, true)
    }

    override fun visit(forStatement: ForStatement) = scoped {
        if (stmAnalysis.isUnreachable(forStatement)) {
            return@scoped
        }
        val bodyBlock = ir.createLabel()
        stmtStack.scoped(ForLoopStmtInfo()) { loopStmtInfo ->
            visitInit(forStatement.init)

            val conditionBlock = loopStmtInfo.resolveCondition(ir)
            ir.branch(conditionBlock)
            ir.switchLabel(conditionBlock)
            val cond = forStatement.condition

            val condition = when (cond) {
                is EmptyExpression -> TrueBoolValue
                else -> makeConditionFromExpression(cond)
            }
            val endBlock = loopStmtInfo.resolveExit(ir)
            ir.branchCond(condition, bodyBlock, endBlock)
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

            ir.switchLabel(endBlock)
        }
    }

    override fun visit(switchStatement: SwitchStatement) = scoped {
        if (stmAnalysis.isUnreachable(switchStatement)) {
            return@scoped
        }
        val condition = visitExpression(switchStatement.condition, true)
        val cvtCond = when (condition.type()) {
            PtrType -> ir.ptr2int(condition, I64Type)
            else -> condition
        }

        val conditionBlock = ir.currentLabel()
        stmtStack.scoped(SwitchStmtInfo(cvtCond.type().asType(), conditionBlock, arrayListOf(), arrayListOf())) { info ->
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
            ir.switch(cvtCond, default, info.values, info.table)

            if (info.exit() != null) {
                ir.switchLabel(info.resolveExit(ir))
            }
        }
    }

    override fun visit(declarator: Declarator): Value {
        val varDesc = sema.typeHolder.getVarTypeOrNull(declarator.name())
            ?: throw IRCodeGenError("Variable not found: '${declarator.name()}'", declarator.begin())
        if (varDesc.storageClass == StorageClass.STATIC) {
            return generateGlobalDeclarator(varDesc, declarator)
        }

        val irType = when (val cType = varDesc.cType()) {
            is BOOL                          -> I8Type
            is CAggregateType, is CPrimitive -> mb.toIRType<NonTrivialType>(sema.typeHolder, cType)
            else -> throw IRCodeGenError("Unknown type, type=$cType", declarator.begin())
        }

        val rvalueAdr = ir.alloc(irType)
        vregStack[declarator.name()] = rvalueAdr
        return rvalueAdr
    }

    private fun zeroMemory(address: Value, type: StructType, range: IntRange) {
        for (i in range) {
            val elementAdr = ir.gfp(address, type, I64Value.of(i))
            when (val f = type.field(i)) {
                is PrimitiveType -> ir.store(elementAdr, PrimitiveConstant.of(f, 0))
                is StructType -> zeroMemory(elementAdr, f)
                is ArrayType -> zeroMemory(elementAdr, f)
            }
        }
    }

    private fun zeroMemory(address: Value, type: StructType) {
        zeroMemory(address, type, type.fields.indices)
    }

    private fun zeroMemory(address: Value, type: ArrayType, range: IntRange) = when (val elementType = type.elementType()) {
        is PrimitiveType -> {
            for (i in range) {
                val elementAdr = ir.gep(address, elementType, I64Value.of(i))
                ir.store(elementAdr, PrimitiveConstant.of(elementType, 0))
            }
        }
        is ArrayType -> {
            for (i in range) {
                val elementAdr = ir.gep(address, elementType, I64Value.of(i))
                zeroMemory(elementAdr, elementType)
            }
        }
        is StructType -> {
            for (i in range) {
                val elementAdr = ir.gep(address, elementType, I64Value.of(i))
                zeroMemory(elementAdr, elementType)
            }
        }
    }

    private fun zeroMemory(address: Value, type: ArrayType) {
        zeroMemory(address, type, 0 until type.length)
    }

    private fun zeroingGaps(value: Value, type: AggregateType, filledPositions: List<Int>) = when (type) {
        is StructType -> {
            for ((left, right) in filledPositions.windowed(2)) {
                if (right - left == 1) continue

                zeroMemory(value, type, left + 1 until right)
            }
        }
        is ArrayType -> {
            for ((left, right) in filledPositions.windowed(2)) {
                if (right - left == 1) continue

                zeroMemory(value, type, left + 1 until right)
            }
        }
    }

    private fun visitInitializers(initializerList: InitializerList, lvalueAdr: Value, type: CAggregateType): List<Int> {
        var position = 0
        val filledPositions = arrayListOf(-1)
        for (init in initializerList.initializers) {
            when (init) {
                is SingleInitializer -> {
                    visitSingleInitializer(init, lvalueAdr, type, position)
                    filledPositions.add(position)
                }
                is DesignationInitializer -> {
                    position = visitDesignationInitializer(init, lvalueAdr, type)
                    filledPositions.add(position)
                }
            }

            position += 1
        }

        val initializerListSize = when (type) {
            is CArrayType     -> type.dimension.toInt()
            is CStringLiteral -> type.dimension.toInt()
            is CStructType    -> type.members().size
            is CUnionType     -> 1
            is CUncompletedArrayType -> TODO()
        }
        filledPositions.add(initializerListSize)
        filledPositions.sort()

        return filledPositions
    }

    private fun visitInitializerList(initializerList: InitializerList, lvalueAdr: Value, type: CAggregateType) {
        val filledPositions = visitInitializers(initializerList, lvalueAdr, type)
        if (sema.resolveInitializerList(initializerList) is CStringLiteral) {
            return
        }

        val irType = mb.toIRType<AggregateType>(sema.typeHolder, type)
        zeroingGaps(lvalueAdr, irType, filledPositions)
    }

    private fun visitDesignationInitializer(designationInitializer: DesignationInitializer, value: Value, type: CAggregateType): Int {
        var address: Value = value
        var innerType: CType = type

        for (designator in designationInitializer.designation.designators) {
            when (designator) {
                is ArrayDesignator -> {
                    if (innerType !is CArrayType) {
                        throw IRCodeGenError("Unknown type, type=$innerType", designationInitializer.begin())
                    }

                    val fieldType = mb.toIRType<ArrayType>(sema.typeHolder, innerType)
                    val index = designator.constEval(sema).toInt()

                    innerType = innerType.element().asType()
                    address = ir.gfp(address, fieldType, I64Value.of(index))
                }
                is MemberDesignator -> {
                    if (innerType !is AnyCStructType) {
                        throw IRCodeGenError("Unknown type, type=$innerType", designationInitializer.begin())
                    }

                    val fieldType = mb.toIRType<StructType>(sema.typeHolder, innerType)
                    val member = innerType.fieldByNameOrNull(designator.name()) ?:
                        throw IRCodeGenError("Unknown field, field=${designator.name()}", designationInitializer.begin())

                    innerType = member.cType()
                        .asType(designator.begin())
                    address = ir.gfp(address, fieldType, I64Value.of(member.index))
                }
            }
        }

        when (val initializer = designationInitializer.initializer) {
            is InitializerListInitializer -> visitInitializerList(initializer.list, address, innerType.asType(initializer.begin()))
            is ExpressionInitializer -> {
                val expression = visitExpression(initializer.expr, true)
                val converted = mb.toIRType<Type>(sema.typeHolder, innerType)
                val convertedRvalue = ir.convertRVToType(expression, converted)
                ir.store(address, convertedRvalue)
            }
        }

        return when (val designator = designationInitializer.designation.designators.first()) {
            is ArrayDesignator -> designator.constEval(sema).toInt()
            is MemberDesignator -> {
                if (type !is AnyCStructType) {
                    throw IRCodeGenError("Unknown type, type=$type", designationInitializer.begin())
                }

                val member = type.fieldByNameOrNull(designator.name()) ?:
                throw IRCodeGenError("Unknown field, field=${designator.name()}", designationInitializer.begin())

                member.index
            }
        }
    }

    private fun generateInitDeclaratorExpressionForPrimitive(initializer: Initializer): Value = when (initializer) {
        is InitializerListInitializer -> {
            val rValue = initializer.list
            if (rValue.initializers.size != 1) {
                throw IRCodeGenError("Initializer list with more than one element", rValue.begin())
            }
            val firstInitializer = rValue.initializers.first()
            if (firstInitializer !is SingleInitializer) {
                throw IRCodeGenError("Unknown initializer, initializer=$firstInitializer", rValue.begin())
            }
            visitInitializer(firstInitializer)
        }
        is ExpressionInitializer -> visitExpression(initializer.expr, true)
    }

    override fun visit(initDeclarator: InitDeclarator): Value {
        val varDesc = sema.typeHolder.getVarTypeOrNull(initDeclarator.declarator.name())
            ?: throw IRCodeGenError("Variable '${initDeclarator.declarator.name()}' not found", initDeclarator.declarator.begin())

        if (varDesc.storageClass == StorageClass.STATIC) {
            return generateGlobalAssignmentDeclarator(varDesc, initDeclarator)
        }
        val type = varDesc.cType()
        if (type is CPrimitive) {
            val rvalue = generateInitDeclaratorExpressionForPrimitive(initDeclarator.rvalue)
            val commonType      = mb.toIRType<Type>(sema.typeHolder, type)
            val convertedRvalue = ir.convertRVToType(rvalue, commonType)

            val lvalueAdr = visit(initDeclarator.declarator)
            ir.store(lvalueAdr, convertedRvalue)
            return convertedRvalue
        }

        val lvalueAdr = initDeclarator.declarator.accept(this)
        when (val rvalue = initDeclarator.rvalue) {
            is InitializerListInitializer -> visitInitializerList(rvalue.list, lvalueAdr, varDesc.cType().asType(rvalue.begin()))
            is ExpressionInitializer -> when (val expr = rvalue.expr) {
                is FunctionCall -> {
                    val rValueType = expr.accept(sema)
                    if (rValueType !is AnyCStructType) {
                        throw IRCodeGenError("Unknown function type, type=$rValueType", rvalue.begin())
                    }

                    visitFuncCallForStructType(lvalueAdr, rValueType, expr)
                }
                is StringNode -> {
                    // TODO impl special case for 'zero' string
                    val rvalueResult = visitStringNode(expr)
                    val rvalueType = expr.accept(sema)
                    val irRvalueType = mb.toIRType<AggregateType>(sema.typeHolder, rvalueType)
                    ir.memcpy(lvalueAdr, rvalueResult, U64Value.of(irRvalueType.sizeOf().toLong()))

                    if (rvalueType.size() < type.size()) {
                        val start = ir.gep(lvalueAdr, I8Type, U64Value.of(rvalueType.size().toLong()))
                        zeroMemory(start, ArrayType(I8Type, type.size() - rvalueType.size()))
                    }
                }
                else -> {
                    val rvalueResult = visitExpression(expr, true)
                    val rvalueType = expr.accept(sema)
                    val irRvalueType = mb.toIRType<AggregateType>(sema.typeHolder, rvalueType)
                    ir.memcpy(lvalueAdr, rvalueResult, U64Value.of(irRvalueType.sizeOf().toLong()))
                }
            }
        }
        return lvalueAdr
    }

    companion object {
        private fun evaluateFirstArgIdx(retType: CType): Int {
            if (retType is AnyCStructType && !retType.isSmall()) {
                return 1
            }

            return 0
        }
    }
}

internal class FunGenInitializer(moduleBuilder: ModuleBuilder,
                                 private val functionNode: FunctionNode,
                                 varStack: VarStack<Value>,
                                 nameGenerator: NameGenerator) :
    AbstractIRGenerator(moduleBuilder, SemanticAnalysis(functionNode.typeHolder), varStack, nameGenerator) {
    fun generate() {
        val varDesc = sema.typeHolder.addVar(functionNode.varDescriptor)
        val fnType = varDesc.cType()
            .asType<CFunctionType>(functionNode.begin())

        val parameters = sema.resolveParameterList(functionNode.parameterTypeList())
        val cPrototype = CFunctionPrototypeBuilder(functionNode.begin(), fnType, mb, sema.typeHolder, varDesc.storageClass).build()

        val currentFunction = mb.createFunction(functionNode.name(), cPrototype.returnType, cPrototype.argumentTypes, cPrototype.attributes)
        val funGen = IrGenFunction(mb, sema, vregStack, nameGenerator, functionNode, parameters, currentFunction, fnType)

        funGen.visitFun()
    }
}