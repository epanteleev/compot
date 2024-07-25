package gen

import types.*
import ir.types.*
import ir.value.*
import parser.nodes.*
import common.assertion
import gen.TypeConverter.coerceArguments
import ir.instruction.*
import ir.instruction.Alloc
import ir.module.block.Label
import gen.TypeConverter.convertToType
import gen.TypeConverter.toIRType
import gen.TypeConverter.toIndexType
import gen.consteval.CommonConstEvalContext
import gen.consteval.ConstEvalExpression
import gen.consteval.ConstEvalExpressionInt
import ir.Definitions.QWORD_SIZE
import ir.global.StringLiteralConstant
import ir.instruction.ArithmeticBinaryOp
import ir.module.AnyFunctionPrototype
import ir.module.builder.impl.ModuleBuilder
import ir.module.builder.impl.FunctionDataBuilder
import parser.nodes.visitors.DeclaratorVisitor
import parser.nodes.visitors.StatementVisitor


class IrGenFunction(moduleBuilder: ModuleBuilder,
                    typeHolder: TypeHolder,
                    varStack: VarStack,
                    constantCounter: Int):
    AbstractIRGenerator(moduleBuilder, typeHolder, varStack, constantCounter),
    StatementVisitor<Unit>,
    DeclaratorVisitor<Value> {
    private var currentFunction: FunctionDataBuilder? = null
    private var returnValueAdr: Alloc? = null
    private var exitBlock: Label = Label.entry //TODO late initialization
    private var stringTolabel = mutableMapOf<String, Label>()
    private val stmtStack = StmtStack()

    private val ir: FunctionDataBuilder
        get() = currentFunction ?: throw IRCodeGenError("Function expected")
    

    private fun visitDeclaration(declaration: Declaration) {
        declaration.resolveType(typeHolder)

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
            is IntegerType, PointerType -> ir.icmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
            is FloatingPointType -> ir.fcmp(conditionExpr, FloatPredicate.One, Constant.of(type, 0))
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private inline fun<reified T: AnyPredicateType> makeCondition(a: Value, predicate: T, b: Value): Value {
        return when (a.type()) {
            is IntegerType, PointerType -> ir.icmp(a, predicate as IntPredicate, b)
            is FloatingPointType -> ir.fcmp(a, predicate as FloatPredicate, b)
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun visitExpression(expression: Expression, isRvalue: Boolean): Value {
        return when (expression) {
            is BinaryOp     -> visitBinary(expression)
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
            is Conditional -> visitConditional(expression)
            is CharNode -> visitCharNode(expression)
            else -> throw IRCodeGenError("Unknown expression: $expression")
        }
    }

    private fun visitCharNode(charNode: CharNode): Value {
        val char = charNode.toInt()
        val charType = charNode.resolveType(typeHolder)
        val charValue = Constant.of(Type.I8, char)
        return ir.convertToType(charValue, mb.toIRType<PrimitiveType>(typeHolder, charType))
    }

    private fun visitConditional(conditional: Conditional): Value {
        val commonType = mb.toIRType<Type>(typeHolder, conditional.resolveType(typeHolder))
        if (commonType == Type.Void) {
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

        } else {
            val onTrue    = visitExpression(conditional.eTrue, true)
            val onFalse   = visitExpression(conditional.eFalse, true)
            val condition = makeConditionFromExpression(conditional.cond)
            commonType as PrimitiveType
            val onTrueConverted = ir.convertToType(onTrue, commonType)
            val onFalseConverted = ir.convertToType(onFalse, commonType)
            return ir.select(condition, commonType, onTrueConverted, onFalseConverted)
        }
    }

    private fun visitInitializerList(ptr: Value, type: AggregateType, initializerList: InitializerList): Value {
        for ((idx, init) in initializerList.initializers.withIndex()) {
            val indexes = arrayOf(Constant.valueOf<IntegerConstant>(Type.I64, idx))
            val fieldPtr = ir.gfp(ptr, type, indexes)
            val value = when (init) {
                is InitializerList -> visitInitializerList(fieldPtr, type.field(idx) as AggregateType, init)
                else -> visitExpression(init, true)
            }

            when (val field = type.field(idx)) {
                is PrimitiveType -> {
                    val converted = ir.convertToType(value, field)
                    ir.store(fieldPtr, converted)
                }
                is CompoundType -> {
                    TODO()
                }
                is ArrayType -> {
                    val converted = ir.convertToType(value, field.elementType() as PrimitiveType)
                    ir.store(fieldPtr, converted)
                }
                else -> throw IRCodeGenError("Unknown field type, field=${field}")
            }
        }
        return ptr
    }

    private fun visitArrowMemberAccess(arrowMemberAccess: ArrowMemberAccess, isRvalue: Boolean): Value {
        val struct       = visitExpression(arrowMemberAccess.primary, true)
        val structType   = arrowMemberAccess.primary.resolveType(typeHolder) as CPointerType
        val structIRType = mb.toIRType<StructType>(typeHolder, structType.dereference())

        val baseStructType = structType.dereference() as CBaseStructType
        val member = baseStructType.fieldIndex(arrowMemberAccess.ident.str())

        val indexes = arrayOf(Constant.valueOf<IntegerConstant>(Type.I64, member))
        val gep = ir.gfp(struct, structIRType, indexes)
        if (!isRvalue) {
            return gep
        }

        val memberType = baseStructType.fields()[member].second
        return if (memberType !is CompoundType) {
            val memberIRType = mb.toIRType<PrimitiveType>(typeHolder, memberType)
            ir.load(memberIRType, gep)
        } else {
            gep
        }
    }

    private fun visitMemberAccess(memberAccess: MemberAccess, isRvalue: Boolean): Value {
        val struct = visitExpression(memberAccess.primary, true) //TODO isRvalue???
        val structType = memberAccess.primary.resolveType(typeHolder) as CBaseStructType
        val structIRType = mb.toIRType<StructType>(typeHolder, structType)

        val member = structType.fieldIndex(memberAccess.memberName())

        val indexes = arrayOf(Constant.valueOf<IntegerConstant>(Type.I64, member))
        val gep = ir.gfp(struct, structIRType, indexes)
        if (!isRvalue) {
            return gep
        }
        val memberType = structType.fields()[member].second
        return if (memberType !is CompoundType) {
            val memberIRType = mb.toIRType<PrimitiveType>(typeHolder, memberType)
            ir.load(memberIRType, gep)
        } else {
            gep
        }
    }

    private fun visitSizeOf(sizeOf: SizeOf): Value {
        when (val expr = sizeOf.expr) {
            is TypeName -> {
                val resolved = expr.specifyType(typeHolder)
                val irType = mb.toIRType<NonTrivialType>(typeHolder, resolved)
                return Constant.of(Type.I64, irType.sizeOf())
            }
            is Expression -> {
                val resolved = expr.resolveType(typeHolder)
                val irType = mb.toIRType<NonTrivialType>(typeHolder, resolved)
                return Constant.of(Type.I64, irType.sizeOf())
            }
            else -> throw IRCodeGenError("Unknown sizeOf expression, expr=${expr}")
        }
    }

    private fun visitStringNode(stringNode: StringNode): Value {
        val string = stringNode.str.data()
        val stringLiteral = StringLiteralConstant(createStringLiteralName(), ArrayType(Type.I8, string.length), string)
        return mb.addConstant(stringLiteral)
    }

    private fun visitArrayAccess(arrayAccess: ArrayAccess, isRvalue: Boolean): Value {
        val index = visitExpression(arrayAccess.expr, true)
        val array = visitExpression(arrayAccess.primary, true)

        val arrayType = arrayAccess.resolveType(typeHolder)
        val elementType = mb.toIRType<NonTrivialType>(typeHolder, arrayType)

        val convertedIndex = ir.toIndexType(index)
        val adr = ir.gep(array, elementType, convertedIndex)
        if (!isRvalue) {
            return adr
        }

        return if (arrayType is CompoundType) {
            adr
        } else {
            ir.load(elementType as PrimitiveType, adr)
        }
    }

    private fun convertArg(function: AnyFunctionPrototype, argIdx: Int, expr: Value): Value {
        if (argIdx >= function.arguments().size) {
            if (!function.isVararg) {
                throw IRCodeGenError("Too many arguments in function call '${function.shortName()}'")
            }

            //TODO Prove it?!?
            return when (expr.type()) {
                Type.F32 -> ir.convertToType(expr, Type.F64)
                Type.I8  -> ir.convertToType(expr, Type.I32)
                Type.U8  -> ir.convertToType(expr, Type.U32)
                else -> expr
            }
        }

        val cvt = function.arguments()[argIdx]
        return ir.convertToType(expr, cvt)
    }

    private fun convertFunctionArgs(function: AnyFunctionPrototype, args: List<Expression>): List<Value> {
        val convertedArgs = mutableListOf<Value>()
        for ((idx, argValue) in args.withIndex()) {
            val expr = visitExpression(argValue, true)
            when (val argCType = argValue.resolveType(typeHolder)) {
                is CPrimitiveType, is CPointerType -> {
                    val convertedArg = convertArg(function, idx, expr)
                    convertedArgs.add(convertedArg)
                }
                is CArrayType -> {
                    val type = mb.toIRType<ArrayType>(typeHolder, argCType)
                    val convertedArg = ir.gep(expr, type.elementType() as PrimitiveType, Constant.of(Type.I64, 0))
                    convertedArgs.add(convertedArg)
                }
                is CStructType -> {
                    val type = mb.toIRType<NonTrivialType>(typeHolder, argCType)
                    convertedArgs.addAll(ir.coerceArguments(type, expr))
                }
                else -> throw IRCodeGenError("Unknown type, type=${argCType} in function call")
            }
        }
        return convertedArgs
    }

    private fun visitCast(cast: Cast): Value {
        val value = visitExpression(cast.cast, true)
        val toType = mb.toIRType<Type>(typeHolder, cast.resolveType(typeHolder))
        if (toType == Type.Void) {
            return value
        }

        assertion(toType is NonTrivialType) { "invariant" }
        return ir.convertToType(value, toType)
    }

    private fun visitFunctionCall(functionCall: FunctionCall): Value {
        val function      = mb.findFunction(functionCall.name())
        val convertedArgs = convertFunctionArgs(function, functionCall.args)

        val cont = ir.createLabel()
        val ret = when (val returnType = function.returnType()) {
            Type.Void -> {
                ir.vcall(function, convertedArgs, cont)
                Value.UNDEF
            }
            is PrimitiveType -> ir.call(function, convertedArgs, cont)
            is StructType, is TupleType -> ir.tupleCall(function, convertedArgs, cont)
            else -> TODO("$returnType")
        }
        ir.switchLabel(cont)
        return ret
    }

    private fun eq(type: Type): AnyPredicateType {
        return when (type) {
            is IntegerType       -> IntPredicate.Eq
            is FloatingPointType -> FloatPredicate.Oeq
            is PointerType       -> IntPredicate.Eq
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun ne(type: Type): AnyPredicateType {
        return when (type) {
            is IntegerType       -> IntPredicate.Ne
            is FloatingPointType -> FloatPredicate.One
            is PointerType       -> IntPredicate.Ne
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun gt(type: Type): AnyPredicateType {
        return when (type) {
            is IntegerType       -> IntPredicate.Gt
            is FloatingPointType -> FloatPredicate.Ogt
            is PointerType       -> IntPredicate.Gt
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun lt(type: Type): AnyPredicateType {
        return when (type) {
            is IntegerType       -> IntPredicate.Lt
            is FloatingPointType -> FloatPredicate.Olt
            is PointerType       -> IntPredicate.Lt
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun le(type: Type): AnyPredicateType {
        return when (type) {
            is IntegerType       -> IntPredicate.Le
            is FloatingPointType -> FloatPredicate.Ole
            is PointerType       -> IntPredicate.Le
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun ge(type: Type): AnyPredicateType {
        return when (type) {
            is IntegerType       -> IntPredicate.Ge
            is FloatingPointType -> FloatPredicate.Oge
            is PointerType       -> IntPredicate.Ge
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun makeAlgebraicBinary(binop: BinaryOp, op: ArithmeticBinaryOp): Value {
        val commonType = mb.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

        if (commonType is PointerType) {
            val rvalue     = visitExpression(binop.right, true)
            val rValueType = binop.right.resolveType(typeHolder)
            if (rValueType !is CPrimitiveType) {
                throw IRCodeGenError("Primitive type expected")
            }
            val convertedRValue = ir.convertToType(rvalue, Type.U64)

            val lvalue     = visitExpression(binop.left, true)
            val lValueType = binop.left.resolveType(typeHolder)
            if (lValueType !is CPointerType) {
                throw IRCodeGenError("Pointer type expected")
            }
            val convertedLValue = ir.convertToType(lvalue, Type.U64)

            val size = lValueType.dereference().size()
            val sizeValue = Constant.of(Type.U64, size)
            val mul = ir.arithmeticBinary(convertedRValue, ArithmeticBinaryOp.Mul, sizeValue)

            val result = when (op) {
                ArithmeticBinaryOp.Add -> ir.arithmeticBinary(convertedLValue, ArithmeticBinaryOp.Add, mul)
                ArithmeticBinaryOp.Sub -> ir.arithmeticBinary(convertedLValue, ArithmeticBinaryOp.Sub, mul)
                else -> throw IRCodeGenError("Unsupported operation for pointers: '$op'")
            }
            return ir.convertToType(result, commonType)

        } else {
            val right = visitExpression(binop.right, true)
            val rightConverted = ir.convertToType(right, commonType)

            val left = visitExpression(binop.left, true)
            val leftConverted = ir.convertToType(left, commonType)

            return ir.arithmeticBinary(leftConverted, op, rightConverted)
        }
    }

    private inline fun makeComparisonBinary(binop: BinaryOp, crossinline predicate: (NonTrivialType) -> AnyPredicateType): Value {
        val commonType = mb.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

        val right = visitExpression(binop.right, true)
        val rightConverted = ir.convertToType(right, commonType)

        val left = visitExpression(binop.left, true)
        val leftConverted = ir.convertToType(left, commonType)

        return makeCondition(leftConverted, predicate(commonType), rightConverted)
    }

    private fun visitBinary(binop: BinaryOp): Value {
        return when (binop.opType) {
            BinaryOpType.ADD -> {
                makeAlgebraicBinary(binop, ArithmeticBinaryOp.Add)
            }

            BinaryOpType.SUB -> {
                makeAlgebraicBinary(binop, ArithmeticBinaryOp.Sub)
            }

            BinaryOpType.ASSIGN -> {
                val right = visitExpression(binop.right, true)
                val leftType = binop.left.resolveType(typeHolder)

                if (leftType is CompoundType) {
                    val left = visitExpression(binop.left, false)
                    ir.memcpy(left, right, U64Value(leftType.size().toLong()))

                    right
                } else {
                    val leftIrType = mb.toIRType<NonTrivialType>(typeHolder, leftType)
                    val leftConverted = ir.convertToType(right, leftIrType)

                    val left = visitExpression(binop.left, false)
                    ir.store(left, leftConverted)
                    leftConverted //TODO test it
                }
            }
            BinaryOpType.ADD_ASSIGN -> {
                val right = visitExpression(binop.right, true)
                val leftType = binop.left.resolveType(typeHolder)
                val leftIrType = mb.toIRType<NonTrivialType>(typeHolder, leftType)
                val rightConverted = ir.convertToType(right, leftIrType)

                val left = visitExpression(binop.left, false)
                val loadedLeft = if (leftType is CPrimitiveType) {
                    ir.load(leftIrType as PrimitiveType, left)
                } else {
                    throw IRCodeGenError("Primitive type expected")
                }

                val sum = ir.arithmeticBinary(loadedLeft, ArithmeticBinaryOp.Add, rightConverted)
                ir.store(left, sum)
                sum // TODO unchecked !!!
            }

            BinaryOpType.BIT_OR -> {
                makeAlgebraicBinary(binop, ArithmeticBinaryOp.Or)
            }

            BinaryOpType.MUL -> {
                makeAlgebraicBinary(binop, ArithmeticBinaryOp.Mul)
            }

            BinaryOpType.NE -> {
                makeComparisonBinary(binop, ::ne)
            }

            BinaryOpType.GT -> {
                makeComparisonBinary(binop, ::gt)
            }
            BinaryOpType.LT -> {
                makeComparisonBinary(binop, ::lt)
            }

            BinaryOpType.LE -> {
                makeComparisonBinary(binop, ::le)
            }
            BinaryOpType.AND -> {
                val initialBB = ir.currentLabel()

                val left = visitExpression(binop.left, true)
                assertion(left.type() == Type.U1) { "expects"}

                val bb = ir.createLabel()

                val end = ir.createLabel()
                ir.branchCond(left, bb, end)
                ir.switchLabel(bb)

                val right = visitExpression(binop.right, true)
                val convertedRight = ir.convertToType(right, Type.U8)
                assertion(right.type() == Type.U1) { "expects"}

                ir.branch(end)
                ir.switchLabel(end)
                ir.phi(listOf(U8Value(0), convertedRight), listOf(initialBB, bb))
            }
            BinaryOpType.OR -> {
                val initialBB = ir.currentLabel()

                val left = visitExpression(binop.left, true)
                assertion(left.type() == Type.U1) { "expects"}

                val bb = ir.createLabel()

                val end = ir.createLabel()
                ir.branchCond(left, end, bb)
                ir.switchLabel(bb)

                val right = visitExpression(binop.right, true)
                val convertedRight = ir.convertToType(right, Type.U8)
                assertion(right.type() == Type.U1) { "expects"}

                ir.branch(end)
                ir.switchLabel(end)
                ir.phi(listOf(U8Value(1), convertedRight), listOf(initialBB, bb))
            }
            BinaryOpType.GE -> {
                makeComparisonBinary(binop, ::ge)
            }
            BinaryOpType.EQ -> {
                makeComparisonBinary(binop, ::eq)
            }
            BinaryOpType.SHL -> {
                makeAlgebraicBinary(binop, ArithmeticBinaryOp.Shl)
            }
            BinaryOpType.SHR -> {
                makeAlgebraicBinary(binop, ArithmeticBinaryOp.Shr)
            }
            BinaryOpType.BIT_AND -> {
                makeAlgebraicBinary(binop, ArithmeticBinaryOp.And)
            }
            BinaryOpType.BIT_XOR -> {
                makeAlgebraicBinary(binop, ArithmeticBinaryOp.Xor)
            }
            BinaryOpType.MOD -> {
                val commonType = mb.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val left = visitExpression(binop.left, true)
                val leftConverted = ir.convertToType(left, commonType)

                val right = visitExpression(binop.right, true)
                val rightConverted = ir.convertToType(right, commonType)

                val rem = ir.tupleDiv(leftConverted, rightConverted)
                ir.proj(rem, 1)
            }
            BinaryOpType.DIV -> {
                makeAlgebraicBinary(binop, ArithmeticBinaryOp.Div)
            }
            else -> throw IRCodeGenError("Unknown binary operation, op='${binop.opType}'")
        }
    }

    private fun visitIncOrDec(unaryOp: UnaryOp, op: ArithmeticBinaryOp): Value {
        assertion(unaryOp.opType == PostfixUnaryOpType.INC || unaryOp.opType == PostfixUnaryOpType.DEC) {
            "Unknown operation, op=${unaryOp.opType}"
        }
        assertion(op == ArithmeticBinaryOp.Add || op == ArithmeticBinaryOp.Sub) {
            "Unknown operation, op=${op}"
        }

        val ctype = unaryOp.resolveType(typeHolder)

        val addr = visitExpression(unaryOp.primary, false)
        val type = mb.toIRType<PrimitiveType>(typeHolder, ctype)
        val loaded = ir.load(type, addr)
        if (ctype is CPointerType) {
            val converted = ir.convertToType(loaded, Type.I64)
            val inc = ir.arithmeticBinary(converted, op, Constant.of(Type.I64, ctype.dereference().size()))
            ir.store(addr, ir.convertToType(inc, type))
        } else {
            val inc = ir.arithmeticBinary(loaded, op, Constant.of(loaded.type(), 1))
            ir.store(addr, ir.convertToType(inc, type))
        }
        return loaded
    }

    private fun visitPrefixIncOrDec(unaryOp: UnaryOp, op: ArithmeticBinaryOp): Value {
        assertion(unaryOp.opType == PrefixUnaryOpType.INC || unaryOp.opType == PrefixUnaryOpType.DEC) {
            "Unknown operation, op=${unaryOp.opType}"
        }
        assertion(op == ArithmeticBinaryOp.Add || op == ArithmeticBinaryOp.Sub) {
            "Unknown operation, op=${op}"
        }

        val ctype = unaryOp.resolveType(typeHolder)

        val addr = visitExpression(unaryOp.primary, false)
        val type = mb.toIRType<PrimitiveType>(typeHolder, ctype)
        val loaded = ir.load(type, addr)
        if (ctype is CPointerType) {
            val converted = ir.convertToType(loaded, Type.I64)
            val inc = ir.arithmeticBinary(converted, op, Constant.of(Type.I64, ctype.dereference().size()))
            ir.store(addr, ir.convertToType(inc, type))
            return inc
        } else {
            val inc = ir.arithmeticBinary(loaded, op, Constant.of(loaded.type(), 1))
            ir.store(addr, ir.convertToType(inc, type))
            return inc
        }
    }

    private fun visitUnary(unaryOp: UnaryOp, isRvalue: Boolean): Value {
        return when (unaryOp.opType) {
            PrefixUnaryOpType.ADDRESS -> visitExpression(unaryOp.primary, false)

            PrefixUnaryOpType.DEREF -> {
                val addr = visitExpression(unaryOp.primary, true)
                val type = unaryOp.resolveType(typeHolder)

                val loadedType = mb.toIRType<PrimitiveType>(typeHolder, type)
                if (isRvalue) {
                    ir.load(loadedType, addr)
                } else {
                    addr
                }
            }
            PostfixUnaryOpType.INC -> visitIncOrDec(unaryOp, ArithmeticBinaryOp.Add)
            PostfixUnaryOpType.DEC -> visitIncOrDec(unaryOp, ArithmeticBinaryOp.Sub)
            PrefixUnaryOpType.INC  -> visitPrefixIncOrDec(unaryOp, ArithmeticBinaryOp.Add)
            PrefixUnaryOpType.DEC  -> visitPrefixIncOrDec(unaryOp, ArithmeticBinaryOp.Sub)
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
                val commonType = mb.toIRType<NonTrivialType>(typeHolder, type)
                val converted = ir.convertToType(value, commonType)
                makeCondition(converted, eq(commonType), Constant.of(converted.type(), 0))
            }
            else -> throw IRCodeGenError("Unknown unary operation, op=${unaryOp.opType}")
        }
    }

    private fun visitNumNode(numNode: NumNode): Constant {
        return when (val num = numNode.number.toNumberOrNull()) {
            is Byte   -> Constant.of(Type.I8, num as Number)
            is UByte  -> Constant.of(Type.U8, num.toLong())
            is Int    -> Constant.of(Type.I32, num as Number)
            is UInt   -> Constant.of(Type.U32, num.toLong())
            is Long   -> Constant.of(Type.I64, num as Number)
            is ULong  -> Constant.of(Type.U64, num.toLong())
            is Float  -> Constant.of(Type.F32, num as Number)
            is Double -> Constant.of(Type.F64, num)
            else -> throw IRCodeGenError("Unknown number type, num=${numNode.number.str()}")
        }
    }

    private fun visitVarNode(varNode: VarNode, isRvalue: Boolean): Value {
        val name = varNode.name()
        val rvalueAttr = varStack[name] ?: throw IRCodeGenError("Variable $name not found")
        val type = typeHolder[name]

        if (type is CArrayType) {
            return rvalueAttr
        }
        val converted = mb.toIRType<NonTrivialType>(typeHolder, type)
        if (!isRvalue) {
            return rvalueAttr
        }
        return if (type !is CompoundType) {
            ir.load(converted as PrimitiveType, rvalueAttr)
        } else {
            rvalueAttr
        }
    }

    private fun argumentTypes(ctypes: List<CType>): List<Type> {
        val types = arrayListOf<Type>()
        for (type in ctypes) {
            when (type) {
                is CPrimitiveType, is CPointerType -> types.add(mb.toIRType<PrimitiveType>(typeHolder, type))
                is CStructType -> {
                    val irType = mb.toIRType<StructType>(typeHolder, type)
                    val parameters = CallConvention.coerceArgumentTypes(irType)
                    if (parameters != null) {
                        types.addAll(parameters)
                    } else {
                        types.add(Type.Ptr)
                    }
                }
                is CArrayType -> {
                    types.add(Type.Ptr)
                }
                else -> throw IRCodeGenError("Unknown type, type=$type")
            }
        }
        return types
    }

    private fun visitParameters(parameters: List<String>,
                                cTypes: List<CType>,
                                arguments: List<ArgumentValue>,
                                closure: (String, CType, List<ArgumentValue>) -> Unit) {
        var currentArg = 0
        while (currentArg < arguments.size) {
            when (val cType = cTypes[currentArg]) {
                is CPrimitiveType, is CPointerType -> {
                    closure(parameters[currentArg], cType, listOf(arguments[currentArg]))
                    currentArg++
                }
                is CStructType -> {
                    val types = CallConvention.coerceArgumentTypes(mb.toIRType<StructType>(typeHolder, cType)) ?: listOf(Type.Ptr)
                    val args = mutableListOf<ArgumentValue>()
                    for (i in types.indices) {
                        args.add(arguments[currentArg + i])
                    }
                    closure(parameters[currentArg], cType, args)
                    currentArg += types.size
                }
                is CArrayType -> {
                    closure(parameters[currentArg], cType, listOf(arguments[currentArg]))
                    currentArg++
                }
                else -> throw IRCodeGenError("Unknown type, type=$cType")
            }
        }
    }

    private fun visitParameter(param: String, cType: CType, args: List<ArgumentValue>) = when (cType) {
        is CPrimitiveType, is CPointerType -> {
            assertion(args.size == 1) { "invariant" }

            val irType    = mb.toIRType<NonTrivialType>(typeHolder, cType)
            val rvalueAdr = ir.alloc(irType)
            ir.store(rvalueAdr, ir.convertToType(args[0], irType))
            varStack[param] = rvalueAdr
        }
        is CStructType -> {
            if (cType.size() <= QWORD_SIZE * 2) {
                val irType    = mb.toIRType<NonTrivialType>(typeHolder, cType)
                val rvalueAdr = ir.alloc(irType)
                args.forEachIndexed { idx, arg ->
                    val offset   = (idx * QWORD_SIZE) / arg.type().sizeOf()
                    val fieldPtr = ir.gep(rvalueAdr, arg.type(), Constant.valueOf(Type.I64, offset))
                    ir.store(fieldPtr, arg)
                }
                varStack[param] = rvalueAdr
            } else {
                assertion(args.size == 1) { "invariant" }
                varStack[param] = args[0]
            }
        }
        is CArrayType -> {
            assertion(args.size == 1) { "invariant" }
            varStack[param] = args[0]
        }
        else -> throw IRCodeGenError("Unknown type, type=$cType")
    }

    private fun emitReturnType(retCType: CType) {
        exitBlock = ir.createLabel()
        if (retCType == CType.VOID) {
            ir.switchLabel(exitBlock)
            ir.retVoid()
            return
        }
        val retType = mb.toIRType<NonTrivialType>(typeHolder, retCType)
        returnValueAdr = ir.alloc(retType)
        ir.switchLabel(exitBlock)
        emitReturn(retType, returnValueAdr!!)
    }

    private fun emitReturn(retType: Type, value: Value) {
        when (retType) {
            is PrimitiveType -> {
                val ret = ir.load(retType, value)
                ir.ret(retType, arrayOf(ret))
            }
            is StructType -> {
                val retValues = ir.coerceArguments(retType, value)
                val retTupleType = CallConvention.coerceArgumentTypes(retType)
                if (retTupleType != null) {
                    if (retTupleType.size == 1) {
                        ir.ret(retTupleType[0], retValues.toTypedArray())
                    } else {
                        ir.ret(TupleType(retTupleType.toTypedArray()), retValues.toTypedArray())
                    }
                } else {
                    ir.ret(Type.Ptr, retValues.toTypedArray())
                }
            }
            else -> throw IRCodeGenError("Unknown return type, type=$retType")
        }
    }

    override fun visit(functionNode: FunctionNode): Value = varStack.scoped {
        val parameters = functionNode.functionDeclarator().params()
        val fnType  = functionNode.declareType(functionNode.specifier, typeHolder)
        val retType = fnType.retType()
        val irRetType = if (retType is CStructType && retType.size() <= QWORD_SIZE * 2) {
            val structType = mb.toIRType<StructType>(typeHolder, retType)
            val list = CallConvention.coerceArgumentTypes(structType) ?: listOf(Type.Ptr)
            if (list.size == 1) {
                list[0]
            } else {
                TupleType(list.toTypedArray())
            }
        } else {
            mb.toIRType<Type>(typeHolder, retType)
        }

        val argTypes = argumentTypes(fnType.args())
        currentFunction = mb.createFunction(functionNode.name(), irRetType, argTypes)

        visitParameters(parameters, fnType.args(), ir.arguments()) { param, cType, args ->
            visitParameter(param, cType, args)
        }

        emitReturnType(retType)

        ir.switchLabel(Label.entry)
        visitStatement(functionNode.body)

        if (ir.last() !is TerminateInstruction) {
            ir.branch(exitBlock)
        }
        return@scoped Value.UNDEF
    }

    private fun visitStatement(statement: Statement) {
        statement.accept(this)
    }

    override fun visit(emptyStatement: EmptyStatement) {}

    override fun visit(exprStatement: ExprStatement) {
        visitExpression(exprStatement.expr, true)
    }

    override fun visit(labeledStatement: LabeledStatement) {
        val label = stringTolabel[labeledStatement.label.str()] ?: throw IRCodeGenError("Label '${labeledStatement.label.str()}' not found ")
        ir.branch(label)
        ir.switchLabel(label)
        visitStatement(labeledStatement.stmt)
    }

    override fun visit(gotoStatement: GotoStatement) {
        val label = stringTolabel[gotoStatement.id.str()] ?: throw IRCodeGenError("Label '${gotoStatement.id.str()}' not found ")
        ir.branch(label)
        ir.switchLabel(label)
    }

    override fun visit(continueStatement: ContinueStatement) {
        val loopInfo = stmtStack.topLoop() ?: throw IRCodeGenError("Continue statement outside of loop")
        ir.branch(loopInfo.continueBB)
    }

    override fun visit(breakStatement: BreakStatement) {
        val loopInfo = stmtStack.topSwitchOrLoop() ?: throw IRCodeGenError("Break statement outside of loop or switch")
        when (loopInfo) {
            is SwitchStmtInfo -> ir.branch(loopInfo.exitBB)
            is LoopStmtInfo   -> ir.branch(loopInfo.exitBB)
            else -> throw IRCodeGenError("Unknown loop info, loopInfo=${loopInfo}")
        }
    }

    override fun visit(defaultStatement: DefaultStatement) {
        val switchInfo = stmtStack.top() as SwitchStmtInfo
        ir.switchLabel(switchInfo.default)
        visitStatement(defaultStatement.stmt)
    }

    override fun visit(caseStatement: CaseStatement) {
        val switchInfo = stmtStack.top() as SwitchStmtInfo

        val ctx = CommonConstEvalContext<Int>(typeHolder)
        val constant = ConstEvalExpression.eval(caseStatement.constExpression, ConstEvalExpressionInt(ctx))

        val caseValueConverted = I32Value(constant)
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
        val expr = returnStatement.expr
        if (expr is EmptyExpression) {
            ir.branch(exitBlock)
            return
        }
        val value = visitExpression(expr, true)
        val realType = ir.prototype().returnType()
        when (val type = returnStatement.expr.resolveType(typeHolder)) {
            is CPrimitiveType, is CPointerType -> {
                val returnType = ir.convertToType(value, realType)
                ir.store(returnValueAdr!!, returnType)
            }
            is CStructType -> {
                ir.memcpy(returnValueAdr!!, value, U64Value(type.size().toLong()))
            }
            else -> throw IRCodeGenError("Unknown return type, type=${returnStatement.expr.resolveType(typeHolder)}")
        }

        ir.branch(exitBlock)
    }

    override fun visit(compoundStatement: CompoundStatement) = varStack.scoped {
        for (node in compoundStatement.statements) {
            if (node !is LabeledStatement) {
                continue
            }

            val label = ir.createLabel()
            stringTolabel[node.label.str()] = label
        }

        for (node in compoundStatement.statements) {
            when (node) {
                is Declaration -> visitDeclaration(node)
                is GotoStatement -> node.accept(this)
                is Statement -> visitStatement(node)
                else -> throw IRCodeGenError("Statement expected")
            }
        }
    }

    override fun visit(ifStatement: IfStatement) = varStack.scoped {
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

    override fun visit(doWhileStatement: DoWhileStatement) = varStack.scoped {
        val bodyBlock = ir.createLabel()
        val endBlock = ir.createLabel()
        val conditionBlock = ir.createLabel()
        stmtStack.push(LoopStmtInfo(conditionBlock, endBlock))

        ir.branch(bodyBlock)
        ir.switchLabel(bodyBlock)

        visitStatement(doWhileStatement.body)
        if (ir.last() !is TerminateInstruction) {
            ir.branch(conditionBlock)
        }
        ir.switchLabel(conditionBlock)

        val condition = makeConditionFromExpression(doWhileStatement.condition)
        ir.branchCond(condition, bodyBlock, endBlock)
        ir.switchLabel(endBlock)
        stmtStack.pop()
    }

    override fun visit(whileStatement: WhileStatement) = varStack.scoped {
        val conditionBlock = ir.createLabel()
        val bodyBlock = ir.createLabel()
        val endBlock = ir.createLabel()
        stmtStack.push(LoopStmtInfo(conditionBlock, endBlock))

        ir.branch(conditionBlock)
        ir.switchLabel(conditionBlock)
        val condition = makeConditionFromExpression(whileStatement.condition)

        ir.branchCond(condition, bodyBlock, endBlock)
        ir.switchLabel(bodyBlock)
        visitStatement(whileStatement.body)
        if (ir.last() !is TerminateInstruction) {
            ir.branch(conditionBlock)
        }
        ir.switchLabel(endBlock)
        stmtStack.pop()
    }

    private fun visitInit(init: Node) {
        when (init) {
            is Declaration    -> visitDeclaration(init)
            is ExprStatement  -> visitExpression(init.expr, true)
            is EmptyStatement -> {}
            else -> throw IRCodeGenError("Unknown init statement, init=$init")
        }
    }

    private fun visitUpdate(update: Expression) {
        if (update is EmptyExpression) {
            return
        }

        visitExpression(update, true)
    }

    override fun visit(forStatement: ForStatement) = varStack.scoped {
        val conditionBlock = ir.createLabel()
        val bodyBlock = ir.createLabel()
        val endBlock = ir.createLabel()
        stmtStack.push(LoopStmtInfo(conditionBlock, endBlock))

        visitInit(forStatement.init)
        ir.branch(conditionBlock)
        ir.switchLabel(conditionBlock)
        val condition = makeConditionFromExpression(forStatement.condition)
        ir.branchCond(condition, bodyBlock, endBlock)
        ir.switchLabel(bodyBlock)
        visitStatement(forStatement.body)
        // TODO update block
        visitUpdate(forStatement.update)
        if (ir.last() !is TerminateInstruction) {
            ir.branch(conditionBlock)
        }
        ir.switchLabel(endBlock)
        stmtStack.pop()
    }

    override fun visit(switchStatement: SwitchStatement) {
        val condition = visitExpression(switchStatement.condition, true)
        val conditionBlock = ir.currentLabel()
        val endBlock = ir.createLabel()
        val defaultBlock = ir.createLabel()
        val info = stmtStack.push(SwitchStmtInfo(endBlock, condition, defaultBlock, arrayListOf(), arrayListOf()))

        visitStatement(switchStatement.body)
        if (ir.last() !is TerminateInstruction) {
            ir.branch(endBlock)
        }

        ir.switchLabel(conditionBlock)
        ir.switch(condition, defaultBlock, info.values, info.table)

        ir.switchLabel(endBlock)
        stmtStack.pop()
    }

    override fun visit(declarator: Declarator): Alloc {
        val type    = typeHolder[declarator.name()]
        val varName = declarator.name()

        val irType        = mb.toIRType<NonTrivialType>(typeHolder, type)
        val rvalueAdr     = ir.alloc(irType)
        varStack[varName] = rvalueAdr
        return rvalueAdr
    }

    override fun visit(initDeclarator: InitDeclarator): Value {
        val type = typeHolder[initDeclarator.name()]
        if (type is CompoundType) {
            val lvalueAdr = initDeclarator.declarator.accept(this)
            when (initDeclarator.rvalue) {
                is InitializerList -> {
                    val typeIr = mb.toIRType<AggregateType>(typeHolder, type)
                    visitInitializerList(lvalueAdr, typeIr, initDeclarator.rvalue)
                    return lvalueAdr
                }
                is FunctionCall -> {
                    val rvalue = visitExpression(initDeclarator.rvalue, true)
                    when (val rType = initDeclarator.rvalue.resolveType(typeHolder)) {
                        is CPrimitiveType, is CPointerType -> {
                            val converted = ir.convertToType(rvalue, mb.toIRType<PrimitiveType>(typeHolder, rType))
                            ir.store(lvalueAdr, converted)
                            return lvalueAdr
                        }
                        is CStructType -> {
                            val structType = mb.toIRType<StructType>(typeHolder, rType)
                            val list = CallConvention.coerceArgumentTypes(structType)
                            if (list == null) {
                                ir.memcpy(lvalueAdr, rvalue, U64Value(rType.size().toLong()))
                                return lvalueAdr
                            }

                            if (list.size == 1) {
                                val gep = ir.gep(lvalueAdr, mb.toIRType<StructType>(typeHolder, rType), Constant.of(Type.I64, 0))
                                ir.store(gep, rvalue)
                            } else {
                                list.forEachIndexed { idx, arg ->
                                    val offset   = (idx * QWORD_SIZE) / arg.sizeOf()
                                    val fieldPtr = ir.gep(lvalueAdr, arg, Constant.valueOf(Type.I64, offset))
                                    val proj = ir.proj(rvalue, idx)
                                    ir.store(fieldPtr, proj)
                                }
                            }

                            return lvalueAdr
                        }
                        else -> throw IRCodeGenError("Unknown type, type=$rType")
                    }
                }
                else -> {
                    val rvalue = visitExpression(initDeclarator.rvalue, true)
                    val commonType = mb.toIRType<NonTrivialType>(typeHolder, type)
                    ir.memcpy(lvalueAdr, rvalue, U64Value(commonType.sizeOf().toLong()))
                    return lvalueAdr
                }
            }
        } else {
            val rvalue = visitExpression(initDeclarator.rvalue, true)
            val commonType = mb.toIRType<NonTrivialType>(typeHolder, type)
            val convertedRvalue = ir.convertToType(rvalue, commonType)

            val lvalueAdr = initDeclarator.declarator.accept(this)
            ir.store(lvalueAdr, convertedRvalue)
            return convertedRvalue
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