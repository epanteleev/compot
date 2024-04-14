package gen

import gen.TypeConverter.convertToType
import ir.*
import ir.instruction.Alloc
import ir.instruction.ArithmeticBinaryOp
import ir.instruction.FloatPredicate
import ir.instruction.IntPredicate
import ir.module.block.Label
import ir.module.builder.impl.FunctionDataBuilder
import ir.module.builder.impl.ModuleBuilder
import ir.types.*
import parser.nodes.*
import types.*


class IrGenFunction(private val moduleBuilder: ModuleBuilder, functionNode: FunctionNode) {
    private val typeHolder = TypeHolder.default()
    private val varStack = VarStack()
    private var currentFunction: FunctionDataBuilder? = null
    private var returnValueAdr: Alloc
    private val exitBlock: Label
    private var stringTolabel = mutableMapOf<String, Label>()

    private fun ir(): FunctionDataBuilder {
        return currentFunction ?: throw IRCodeGenError("Function expected")
    }

    private fun toIRType(type: CType): NonTrivialType {
        for (p in type.qualifiers()) {
            if (p is PointerQualifier) {
                return Type.Ptr
            }
        }
        if (type is CPointerType) {
            return Type.Ptr
        }
        return when (type.baseType()) {
            CPrimitive.CHAR -> Type.I8
            CPrimitive.SHORT -> Type.I16
            CPrimitive.INT -> Type.I32
            CPrimitive.LONG -> Type.I64
            CPrimitive.FLOAT -> Type.F32
            CPrimitive.DOUBLE -> Type.F64
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    private fun visitStatement(statement: Statement): Boolean {
        return when (statement) {
            is CompoundStatement -> visitCompoundStatement(statement)
            is ExprStatement -> visitExpressionStatement(statement)
            is ReturnStatement -> visitReturn(statement)
            is IfStatement -> visitIf(statement)
            is WhileStatement -> visitWhile(statement)
            is DoWhileStatement -> visitDoWhile(statement)
            is LabeledStatement -> visitLabeledStatement(statement)
            is GotoStatement -> visitGoto(statement)
            is ForStatement -> visitFor(statement)
            else -> throw IRCodeGenError("Statement expected, but got $statement")
        }
    }

    private fun visitDeclarator(decl: Declarator) {
        val type = typeHolder[decl.name()]
        val varName = decl.name()

        val irType = toIRType(type)
        val rvalueAdr = ir().alloc(irType)
        varStack[varName] = KeyType(type, rvalueAdr)
    }

    private fun visitAssignmentDeclarator(decl: AssignmentDeclarator) {
        val type = typeHolder[decl.name()]
        val varName = decl.name()

        val irType = toIRType(type)
        val rvalueAdr = ir().alloc(irType)
        varStack[varName] = KeyType(type, rvalueAdr)

        val lvalue = visitExpression(decl.lvalue, false)
        ir().store(rvalueAdr, lvalue)
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
        val needSwitch = visitStatement(forStatement.body)
        visitUpdate(forStatement.update)
        if (needSwitch) {
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
        val elseBlock = ir().createLabel()

        if (ifStatement.elseNode is EmptyStatement) {
            ir().branchCond(condition, thenBlock, elseBlock)
            ir().switchLabel(thenBlock)
            val needSwitch = visitStatement(ifStatement.then)
            if (needSwitch) {
                ir().branch(elseBlock)
            }
            ir().switchLabel(elseBlock)
            return true
        } else {
            ir().branchCond(condition, thenBlock, elseBlock)
            // then
            ir().switchLabel(thenBlock)
            val needSwitch = visitStatement(ifStatement.then)
            val switchBl = if (needSwitch) {
                val endBlock = ir().createLabel()
                ir().branch(endBlock)
                endBlock
            } else {
                elseBlock
            }

            // else
            ir().switchLabel(switchBl)
            return visitStatement(ifStatement.elseNode)
        }
    }

    private fun visitExpressionStatement(expr: ExprStatement): Boolean {
        visitExpression(expr.expr, true)
        return true
    }

    private fun visitExpression(expression: Expression, isRvalue: Boolean): Value {
        return when (expression) {
            is BinaryOp -> visitBinary(expression, isRvalue)
            is UnaryOp -> visitUnary(expression, isRvalue)
            is NumNode -> visitNumNode(expression)
            is VarNode -> visitVarNode(expression, isRvalue)
            is FunctionCall -> visitFunctionCall(expression)
            else -> throw IRCodeGenError("Unknown expression: $expression")
        }
    }

    private fun visitFunctionCall(functionCall: FunctionCall): Value {
        val name = functionCall.name()
        val function = moduleBuilder.findFunction(name)

        val convertedArgs = mutableListOf<Value>()
        for ((argType, argValue) in function.arguments() zip functionCall.args) {
            val converted = visitExpression(argValue, true)
            val convertedArg = ir().convertToType(converted, argType)
            convertedArgs.add(convertedArg)
        }

        return ir().call(function, convertedArgs)
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

    private fun visitBinary(binop: BinaryOp, isRvalue: Boolean): Value {
        return when (binop.opType) {
            BinaryOpType.ADD -> {
                val left = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType = toIRType(binop.resolveType(typeHolder))
                val leftConverted = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                ir().arithmeticBinary(leftConverted, ArithmeticBinaryOp.Add, rightConverted)
            }

            BinaryOpType.SUB -> {
                val left = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType = toIRType(binop.resolveType(typeHolder))
                val leftConverted = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                ir().arithmeticBinary(leftConverted, ArithmeticBinaryOp.Sub, rightConverted)
            }

            BinaryOpType.ASSIGN -> {
                val left = visitExpression(binop.left, false)
                val right = visitExpression(binop.right, true)
                val commonType = toIRType(binop.resolveType(typeHolder))
                val rightConverted = ir().convertToType(right, commonType)
                ir().store(left, rightConverted)
                right //TODO
            }

            BinaryOpType.MUL -> {
                val left = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType = toIRType(binop.resolveType(typeHolder))
                val leftConverted = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                ir().arithmeticBinary(leftConverted, ArithmeticBinaryOp.Mul, rightConverted)
            }

            BinaryOpType.NE -> {
                val left = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType = toIRType(binop.resolveType(typeHolder))
                val leftConverted = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                val cmp = ir().icmp(leftConverted, IntPredicate.Ne, rightConverted)
                ir().convertToType(cmp, Type.U1)
            }

            BinaryOpType.GT -> {
                val left = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType = toIRType(binop.resolveType(typeHolder))
                val leftConverted = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                val cmp = ir().icmp(leftConverted, IntPredicate.Gt, rightConverted)
                ir().convertToType(cmp, Type.U1)
            }
            BinaryOpType.LT -> {
                val left = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType = toIRType(binop.resolveType(typeHolder))
                val leftConverted = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                val cmp = ir().icmp(leftConverted, IntPredicate.Lt, rightConverted)
                ir().convertToType(cmp, Type.U1)
            }

            BinaryOpType.LE -> {
                val left = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType = toIRType(binop.resolveType(typeHolder))
                val leftConverted = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                val cmp = ir().icmp(leftConverted, IntPredicate.Le, rightConverted)
                ir().convertToType(cmp, Type.U1)
            }

            else -> throw IRCodeGenError("Unknown binary operation, op=${binop.opType}")
        }
    }

    private fun visitUnary(unaryOp: UnaryOp, isRvalue: Boolean): Value {
        return when (unaryOp.opType) {
            PrefixUnaryOpType.ADDRESS -> visitExpression(unaryOp.primary, false)

            PrefixUnaryOpType.DEREF -> {
                val addr = visitExpression(unaryOp.primary, isRvalue)
                val type = unaryOp.resolveType(typeHolder)

                val loadedType = toIRType(type) as PrimitiveType
                if (isRvalue) {
                    ir().load(loadedType, addr)
                } else {
                    ir().load(Type.Ptr, addr) //TODO
                }
            }
            PostfixUnaryOpType.INC -> {
                val addr = visitExpression(unaryOp.primary, false)
                val loaded = ir().load(Type.I32, addr)
                val inc = ir().arithmeticBinary(loaded, ArithmeticBinaryOp.Add, Constant.of(Type.I32, 1))
                ir().store(addr, inc)
                loaded
            }
            else -> throw IRCodeGenError("Unknown unary operation, op=${unaryOp.opType}")
        }
    }

    private fun visitNumNode(numNode: NumNode): Constant {
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
                ir().store(returnValueAdr, returnType)
                ir().branch(exitBlock)
            }
        }
        return false
    }

    private fun visitVarNode(varNode: VarNode, isRvalue: Boolean): Value {
        val name = varNode.name()
        val rvalueAttr = varStack[name] ?: throw IRCodeGenError("Variable $name not found")
        return if (isRvalue) {
            ir().load(toIRType(rvalueAttr.first) as PrimitiveType, rvalueAttr.second)
        } else {
            rvalueAttr.second
        }
    }

    init {
        val name = functionNode.name()
        val parameters = functionNode.functionDeclarator().params()
        val fnType = functionNode.resolveType(typeHolder)
        val retType = toIRType(fnType.retType)

        currentFunction = moduleBuilder.createFunction(name, retType, fnType.argsTypes.map { toIRType(it) })

        varStack.push()

        for (idx in parameters.indices) {
            val param = parameters[idx]
            val arg = ir().argument(idx)
            val type = fnType.argsTypes[idx]

            val rvalueAdr = ir().alloc(arg.type())
            varStack[param] = KeyType(type, rvalueAdr)
            ir().store(rvalueAdr, arg)
        }

        returnValueAdr = ir().alloc(retType)

        exitBlock = ir().createLabel()
        ir().switchLabel(exitBlock)
        val loadReturn = ir().load(retType as PrimitiveType, returnValueAdr)
        ir().ret(loadReturn)

        ir().switchLabel(Label.entry)
        val needSwitch = visitStatement(functionNode.body)
        if (needSwitch) {
            ir().branch(exitBlock)
        }

        varStack.pop()
    }
}