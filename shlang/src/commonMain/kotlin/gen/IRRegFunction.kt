package gen

import common.assertion
import ir.*
import types.*
import ir.types.*
import parser.nodes.*
import ir.instruction.*
import ir.instruction.Alloc
import ir.module.block.Label
import gen.TypeConverter.convertToType
import gen.TypeConverter.toIRType
import ir.global.StringLiteralGlobal
import ir.instruction.ArithmeticBinaryOp
import ir.module.AnyFunctionPrototype
import ir.module.builder.impl.ModuleBuilder
import ir.module.builder.impl.FunctionDataBuilder
import types.AggregateBaseType


class IrGenFunction(private val moduleBuilder: ModuleBuilder,
                    private val typeHolder: TypeHolder, functionNode: FunctionNode) {
    private val varStack = VarStack()
    private var currentFunction: FunctionDataBuilder? = null
    private var returnValueAdr: Alloc? = null
    private val exitBlock: Label
    private var stringTolabel = mutableMapOf<String, Label>()

    private fun ir(): FunctionDataBuilder {
        return currentFunction ?: throw IRCodeGenError("Function expected")
    }

    private fun visitStatement(statement: Statement): Boolean {
        return when (statement) {
            is CompoundStatement -> visitCompoundStatement(statement)
            is ExprStatement     -> visitExpressionStatement(statement)
            is ReturnStatement   -> visitReturn(statement)
            is IfStatement       -> visitIf(statement)
            is WhileStatement    -> visitWhile(statement)
            is DoWhileStatement  -> visitDoWhile(statement)
            is LabeledStatement  -> visitLabeledStatement(statement)
            is GotoStatement     -> visitGoto(statement)
            is ForStatement      -> visitFor(statement)
            is EmptyStatement    -> false
            else -> throw IRCodeGenError("Statement expected, but got $statement")
        }
    }

    private fun visitDeclarator(decl: Declarator): Alloc {
        val type = typeHolder[decl.name()]
        val varName = decl.name()

        val irType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, type)
        val rvalueAdr = ir().alloc(irType)
        varStack[varName] = rvalueAdr
        return rvalueAdr
    }

    private fun visitAssignmentDeclarator(decl: AssignmentDeclarator) {
        val type = typeHolder[decl.name()]

        val rvalue = visitExpression(decl.rvalue, true)
        val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, type)
        val convertedRvalue = ir().convertToType(rvalue, commonType)

        val lvalueAdr = visitDeclarator(decl.declarator)
        ir().store(lvalueAdr, convertedRvalue)
    }

    private fun visitDeclaration(declaration: Declaration) {
        declaration.resolveType(typeHolder)

        for (decl in declaration.declarators) {
            when (decl) {
                is Declarator -> visitDeclarator(decl)
                is AssignmentDeclarator -> visitAssignmentDeclarator(decl)
                else -> throw IRCodeGenError("Unknown declarator, delc=$decl")
            }
        }
    }

    private fun visitFor(forStatement: ForStatement): Boolean {
        fun visitInit(init: Node) {
            when (init) {
                is Declaration    -> visitDeclaration(init)
                is ExprStatement  -> visitExpression(init.expr, true)
                is EmptyStatement -> {}
                else -> throw IRCodeGenError("Unknown init statement, init=$init")
            }
        }

        fun visitUpdate(update: Expression) {
            if (update is EmptyExpression) {
                return
            }

            visitExpression(update, true)
        }

        val conditionBlock = ir().createLabel()
        val bodyBlock = ir().createLabel()
        val endBlock = ir().createLabel()

        visitInit(forStatement.init)
        ir().branch(conditionBlock)
        ir().switchLabel(conditionBlock)
        val condition = makeCondition(forStatement.condition)
        ir().branchCond(condition, bodyBlock, endBlock)
        ir().switchLabel(bodyBlock)
        visitStatement(forStatement.body)
        visitUpdate(forStatement.update)
        if (ir().last() !is TerminateInstruction) {
            ir().branch(conditionBlock)
        }
        ir().switchLabel(endBlock)
        return true
    }

    private fun visitLabeledStatement(statement: LabeledStatement): Boolean {
        val label = stringTolabel[statement.label.str()] ?: throw IRCodeGenError("Label '${statement.label.str()}' not found ")
        ir().branch(label)
        ir().switchLabel(label)
        return visitStatement(statement.stmt)
    }

    private fun visitGoto(statement: GotoStatement): Boolean {
        val label = stringTolabel[statement.id.str()] ?: throw IRCodeGenError("Label '${statement.id.str()}' not found ")
        ir().branch(label)
        ir().switchLabel(label)
        return true
    }

    private fun makeCondition(condition: Expression): Value {
        val conditionExpr = visitExpression(condition, true)
        if (conditionExpr.type() == Type.U1) {
            return conditionExpr
        }

        return when (val type = conditionExpr.type()) {
            is SignedIntType -> ir().icmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
            is UnsignedIntType -> ir().ucmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
            is FloatingPointType -> ir().fcmp(conditionExpr, FloatPredicate.One, Constant.of(type, 0))
            is PointerType -> ir().pcmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun visitDoWhile(doWhileStatement: DoWhileStatement): Boolean {
        val bodyBlock = ir().createLabel()
        val endBlock = ir().createLabel()

        ir().branch(bodyBlock)
        ir().switchLabel(bodyBlock)
        visitStatement(doWhileStatement.body)

        val condition = makeCondition(doWhileStatement.condition)

        ir().branchCond(condition, bodyBlock, endBlock)
        ir().switchLabel(endBlock)
        return true
    }

    private fun visitWhile(whileStatement: WhileStatement): Boolean {
        val conditionBlock = ir().createLabel()
        val bodyBlock = ir().createLabel()
        val endBlock = ir().createLabel()

        ir().branch(conditionBlock)
        ir().switchLabel(conditionBlock)
        val condition = makeCondition(whileStatement.condition)

        ir().branchCond(condition, bodyBlock, endBlock)
        ir().switchLabel(bodyBlock)
        val needSwitch = visitStatement(whileStatement.body)
        if (needSwitch) {
            ir().branch(conditionBlock)
        }
        ir().switchLabel(endBlock)
        return true
    }

    private fun visitIf(ifStatement: IfStatement): Boolean {
        val condition = makeCondition(ifStatement.condition)

        val thenBlock = ir().createLabel()


        if (ifStatement.elseNode is EmptyStatement) {
            val endBlock = ir().createLabel()
            ir().branchCond(condition, thenBlock, endBlock)
            ir().switchLabel(thenBlock)
            val needSwitch = visitStatement(ifStatement.then)
            if (needSwitch) {
                ir().branch(endBlock)
            }
            ir().switchLabel(endBlock)
            return true
        } else {

            val elseBlock = ir().createLabel()
            ir().branchCond(condition, thenBlock, elseBlock)
            // then
            ir().switchLabel(thenBlock)
            val needSwitch = visitStatement(ifStatement.then)
            val endBlock = if (needSwitch) {
                val endBlock = ir().createLabel()
                ir().branch(endBlock)
                endBlock
            } else {
                null
            }

            // else
            ir().switchLabel(elseBlock)
            val switch1 = visitStatement(ifStatement.elseNode)

            if (switch1) {
                val newEndBlock = endBlock ?: ir().createLabel()
                ir().branch(newEndBlock)
                ir().switchLabel(newEndBlock)
            } else if (endBlock != null) {
                ir().switchLabel(endBlock)
            }

            return true
        }
    }

    private fun visitExpressionStatement(expr: ExprStatement): Boolean {
        visitExpression(expr.expr, true)
        return true
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
            else -> throw IRCodeGenError("Unknown expression: $expression")
        }
    }

    private fun visitArrowMemberAccess(arrowMemberAccess: ArrowMemberAccess, isRvalue: Boolean): Value {
        val struct = visitExpression(arrowMemberAccess.primary, true)
        val structType = arrowMemberAccess.primary.resolveType(typeHolder) as CPointerType
        val structIRType = moduleBuilder.toIRType<StructType>(typeHolder, structType.dereference())

        val baseStructType = structType.baseType() as AggregateBaseType
        val member = baseStructType.fieldIndex(arrowMemberAccess.ident.str())
        val memberType = baseStructType.fields()[member].second
        val gep = ir().gfp(struct, structIRType, Constant.valueOf(Type.I64, member))
        return if (isRvalue) {
            val memberIRType = moduleBuilder.toIRType<PrimitiveType>(typeHolder, memberType)
            ir().load(memberIRType, gep)
        } else {
            gep
        }
    }

    private fun visitMemberAccess(memberAccess: MemberAccess, isRvalue: Boolean): Value {
        val struct = visitExpression(memberAccess.primary, false) //TODO isRvalue???
        val structType = memberAccess.primary.resolveType(typeHolder)
        val structIRType = moduleBuilder.toIRType<StructType>(typeHolder, structType)

        val baseStructType = structType.baseType() as AggregateBaseType
        val member = baseStructType.fieldIndex(memberAccess.ident.str())
        val memberType = baseStructType.fields()[member].second

        val gep = ir().gfp(struct, structIRType, Constant.valueOf(Type.I64, member))
        return if (isRvalue) {
            val memberIRType = moduleBuilder.toIRType<PrimitiveType>(typeHolder, memberType)
            ir().load(memberIRType, gep)
        } else {
            gep
        }
    }

    private fun visitSizeOf(sizeOf: SizeOf): Value {
        when (val expr = sizeOf.expr) {
            is TypeName -> {
                val resolved = expr.resolveType(typeHolder)
                return Constant.of(Type.I64, resolved.size())
            }
            is VarNode -> {
                val resolved = expr.resolveType(typeHolder)
                return Constant.of(Type.I64, resolved.size())
            }
            else -> throw IRCodeGenError("Unknown sizeOf expression, expr=${expr}")
        }
    }

    private fun visitStringNode(stringNode: StringNode): Value {
        // TODO
        val stringLiteral = StringLiteralGlobal("str", ArrayType(Type.I8, 11), stringNode.str.unquote())
        return moduleBuilder.addConstant(stringLiteral)
    }

    private fun visitArrayAccess(arrayAccess: ArrayAccess, isRvalue: Boolean): Value {
        val index = visitExpression(arrayAccess.expr, true)
        val array = visitExpression(arrayAccess.primary, true)

        val arrayType = arrayAccess.resolveType(typeHolder)
        val elementType = moduleBuilder.toIRType<PrimitiveType>(typeHolder, arrayType)

        val adr = ir().gep(array, elementType, index)

        return if (isRvalue) {
            ir().load(elementType,adr)
        } else {
            adr
        }
    }

    private fun convertFunctionArgs(function: AnyFunctionPrototype, args: List<Expression>): List<Value> {
        fun convertArg(argIdx: Int, expr: Value, type: NonTrivialType): Value {
            if (type is ArrayType) {
                return ir().gep(expr, type.elementType() as PrimitiveType, Constant.of(Type.I64, 0))
            }
            if (argIdx >= function.arguments().size) {
                if (!function.isVararg) {
                    throw IRCodeGenError("Too many arguments in function call '${function.shortName()}'")
                }

                return expr
            }

            val cvt = function.arguments()[argIdx]
            return ir().convertToType(expr, cvt)
        }

        val convertedArgs = mutableListOf<Value>()
        for ((idx, argValue) in args.withIndex()) {
            val expr = visitExpression(argValue, true)
            val type = moduleBuilder.toIRType<NonTrivialType>(typeHolder, argValue.resolveType(typeHolder))

            val convertedArg = convertArg(idx, expr, type)
            convertedArgs.add(convertedArg)
        }
        return convertedArgs
    }

    private fun visitCast(cast: Cast): Value {
        val value = visitExpression(cast.cast, true)
        val toType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, cast.resolveType(typeHolder))
        return ir().convertToType(value, toType)
    }

    private fun visitFunctionCall(functionCall: FunctionCall): Value {
        val function = moduleBuilder.findFunction(functionCall.name())
        val convertedArgs = convertFunctionArgs(function, functionCall.args)

        if (function.returnType() == Type.Void) {
            val cont = ir().createLabel()
            ir().vcall(function, convertedArgs, cont)
            ir().switchLabel(cont)
            return Value.UNDEF
        } else {
            val cont = ir().createLabel()
            val ret = ir().call(function, convertedArgs, cont)
            ir().switchLabel(cont)
            return ret
        }
    }

    private fun visitCompoundStatement(compoundStatement: CompoundStatement): Boolean {
        var needSwitch = true

        for (node in compoundStatement.statements) {
            if (node !is LabeledStatement) {
                continue
            }

            val label = ir().createLabel()
            stringTolabel[node.label.str()] = label
        }

        for (node in compoundStatement.statements) {
            when (node) {
                is Declaration -> visitDeclaration(node)
                is GotoStatement -> return visitGoto(node)
                is Statement -> {
                    needSwitch = visitStatement(node)
                }
                else -> throw IRCodeGenError("Statement expected")
            }
        }
        return needSwitch
    }

    private fun visitBinary(binop: BinaryOp): Value {
        return when (binop.opType) {
            BinaryOpType.ADD -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                ir().arithmeticBinary(leftConverted, ArithmeticBinaryOp.Add, rightConverted)
            }

            BinaryOpType.SUB -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                ir().arithmeticBinary(leftConverted, ArithmeticBinaryOp.Sub, rightConverted)
            }

            BinaryOpType.ASSIGN -> {
                val right = visitExpression(binop.right, true)
                val rightType = binop.left.resolveType(typeHolder)
                val rightConverted = ir().convertToType(right, moduleBuilder.toIRType<NonTrivialType>(typeHolder, rightType))

                val left = visitExpression(binop.left, false)
                ir().store(left, rightConverted)
                rightConverted //TODO
            }

            BinaryOpType.MUL -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                ir().arithmeticBinary(leftConverted, ArithmeticBinaryOp.Mul, rightConverted)
            }

            BinaryOpType.NE -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                val cmp = ir().icmp(leftConverted, IntPredicate.Ne, rightConverted)
                ir().convertToType(cmp, Type.U1)
            }

            BinaryOpType.GT -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                val cmp = ir().icmp(leftConverted, IntPredicate.Gt, rightConverted)
                ir().convertToType(cmp, Type.U1)
            }
            BinaryOpType.LT -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                val cmp = ir().icmp(leftConverted, IntPredicate.Lt, rightConverted)
                ir().convertToType(cmp, Type.U1)
            }

            BinaryOpType.LE -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                val cmp = ir().icmp(leftConverted, IntPredicate.Le, rightConverted)
                ir().convertToType(cmp, Type.U1)
            }
            BinaryOpType.AND -> {
                val initialBB = ir().currentLabel()

                val left = visitExpression(binop.left, true)
                assertion(left.type() == Type.U1) { "expects"}

                val bb = ir().createLabel()

                val end = ir().createLabel()
                ir().branchCond(left, bb, end)
                ir().switchLabel(bb)

                val right = visitExpression(binop.right, true)
                val convertedRight = ir().convertToType(right, Type.U8)
                assertion(right.type() == Type.U1) { "expects"}

                ir().branch(end)
                ir().switchLabel(end)
                ir().phi(listOf(U8Value(0), convertedRight), listOf(initialBB, bb))
            }
            BinaryOpType.GE -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                val cmp = ir().icmp(leftConverted, IntPredicate.Ge, rightConverted)

                ir().convertToType(cmp, Type.U1)
            }
            BinaryOpType.EQ -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                val cmp = ir().icmp(leftConverted, IntPredicate.Eq, rightConverted)

                ir().convertToType(cmp, Type.U1)
            }
            BinaryOpType.MOD -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                val rem = ir().tupleDiv(leftConverted, rightConverted)
                ir().proj(rem, 1)
            }
            BinaryOpType.DIV -> {
                val commonType = moduleBuilder.toIRType<NonTrivialType>(typeHolder, binop.resolveType(typeHolder))

                val left = visitExpression(binop.left, true)
                val leftConverted = ir().convertToType(left, commonType)

                val right = visitExpression(binop.right, true)
                val rightConverted = ir().convertToType(right, commonType)

                ir().arithmeticBinary(leftConverted, ArithmeticBinaryOp.Div, rightConverted)
            }
            else -> throw IRCodeGenError("Unknown binary operation, op=${binop.opType}")
        }
    }

    private fun visitUnary(unaryOp: UnaryOp, isRvalue: Boolean): Value {
        return when (unaryOp.opType) {
            PrefixUnaryOpType.ADDRESS -> visitExpression(unaryOp.primary, false)

            PrefixUnaryOpType.DEREF -> {
                val addr = visitExpression(unaryOp.primary, true)
                val type = unaryOp.resolveType(typeHolder)

                val loadedType = moduleBuilder.toIRType<PrimitiveType>(typeHolder, type)
                if (isRvalue) {
                    ir().load(loadedType, addr)
                } else {
                    addr
                }
            }
            PostfixUnaryOpType.INC -> {
                val addr = visitExpression(unaryOp.primary, false)
                val type = moduleBuilder.toIRType<PrimitiveType>(typeHolder, unaryOp.resolveType(typeHolder))
                val loaded = ir().load(type, addr)
                val converted = if (loaded.type() == Type.Ptr) {
                    ir().convertToType(loaded, Type.I64)
                } else {
                    loaded
                }
                val inc = ir().arithmeticBinary(converted, ArithmeticBinaryOp.Add, Constant.of(converted.type(), 1))
                ir().store(addr, ir().convertToType(inc, type))
                loaded
            }
            PostfixUnaryOpType.DEC -> {
                val addr = visitExpression(unaryOp.primary, false)
                val type = moduleBuilder.toIRType<PrimitiveType>(typeHolder, unaryOp.resolveType(typeHolder))
                val loaded = ir().load(type, addr)
                val converted = if (loaded.type() == Type.Ptr) {
                    ir().convertToType(loaded, Type.I64)
                } else {
                    loaded
                }

                val dec = ir().arithmeticBinary(converted, ArithmeticBinaryOp.Sub, Constant.of(converted.type(), 1))
                ir().store(addr, ir().convertToType(dec, type))
                loaded
            }
            PrefixUnaryOpType.NEG -> {
                val value = visitExpression(unaryOp.primary, true)
                val type = unaryOp.resolveType(typeHolder)
                val converted = ir().convertToType(value, moduleBuilder.toIRType<NonTrivialType>(typeHolder, type))
                // TODO: handle case '-1'
                ir().arithmeticBinary(Constant.of(moduleBuilder.toIRType<NonTrivialType>(typeHolder, type), 0), ArithmeticBinaryOp.Sub, converted)
            }
            else -> throw IRCodeGenError("Unknown unary operation, op=${unaryOp.opType}")
        }
    }

    private fun visitNumNode(numNode: NumNode): Constant {
        when (numNode.toLong.data) {
            is Double -> return F64Value(numNode.toLong.data)
            is Float -> return F32Value(numNode.toLong.data)
            else -> {}
        }
        return when (numNode.toLong.data) {
            in 0..255 -> U8Value(numNode.toLong.data.toByte())
            in 0..65535 -> U16Value(numNode.toLong.data.toShort())
            in 0..4294967295 -> U32Value(numNode.toLong.data.toInt())
            in -128..127 -> I8Value(numNode.toLong.data.toByte())
            in -32768..32767 -> I16Value(numNode.toLong.data.toShort())
            in -2147483648..2147483647 -> I32Value(numNode.toLong.data.toInt())
            else -> I64Value(numNode.toLong.data.toLong())
        }
    }

    private fun visitReturn(returnStatement: ReturnStatement): Boolean {
        when (returnStatement.expr) {
            is EmptyExpression -> ir().branch(exitBlock)
            else -> {
                val value = visitExpression(returnStatement.expr, true)
                val realType = ir().prototype().returnType()
                val returnType = ir().convertToType(value, realType)
                ir().store(returnValueAdr!!, returnType)
                ir().branch(exitBlock)
            }
        }
        return false
    }

    private fun visitVarNode(varNode: VarNode, isRvalue: Boolean): Value {
        val name = varNode.name()
        val rvalueAttr = varStack[name] ?: throw IRCodeGenError("Variable $name not found")
        val type = typeHolder[name]

        if (type.baseType() is CArrayType) {
            return rvalueAttr
        }

        return if (isRvalue) {
            ir().load(moduleBuilder.toIRType<PrimitiveType>(typeHolder, type), rvalueAttr)
        } else {
            rvalueAttr
        }
    }

    init {
        val name = functionNode.name()
        val parameters = functionNode.functionDeclarator().params()
        val fnType = functionNode.resolveType(typeHolder)
        val retType = moduleBuilder.toIRType<Type>(typeHolder, fnType.retType())

        currentFunction = moduleBuilder.createFunction(name, retType, fnType.args().map { moduleBuilder.toIRType(typeHolder, it) })

        varStack.push()

        for (idx in parameters.indices) {
            val param = parameters[idx]
            val arg = ir().argument(idx)

            val rvalueAdr = ir().alloc(arg.type())
            varStack[param] = rvalueAdr
            ir().store(rvalueAdr, arg)
        }

        if (retType is NonTrivialType) {
            returnValueAdr = ir().alloc(retType)
            exitBlock = ir().createLabel()
            ir().switchLabel(exitBlock)
            val loadReturn = ir().load(retType as PrimitiveType, returnValueAdr!!)
            ir().ret(loadReturn)
        } else {
            exitBlock = ir().createLabel()
            ir().switchLabel(exitBlock)
            ir().retVoid()
        }

        ir().switchLabel(Label.entry)
        visitStatement(functionNode.body)
        if (ir().last() !is TerminateInstruction) {
            ir().branch(exitBlock)
        }

        varStack.pop()
    }
}