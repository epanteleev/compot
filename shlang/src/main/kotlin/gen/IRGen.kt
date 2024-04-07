package gen

import gen.TypeConverter.convertToType
import ir.*
import ir.instruction.ArithmeticBinaryOp
import ir.instruction.FloatPredicate
import ir.instruction.IntPredicate
import types.*
import ir.module.Module
import ir.module.block.Label
import ir.module.builder.impl.FunctionDataBuilder
import ir.module.builder.impl.ModuleBuilder
import ir.types.*
import parser.LineAgnosticAstPrinter
import parser.nodes.*
import java.lang.Exception


data class IRCodeGenError(override val message: String): Exception(message)

class IRGen private constructor() {
    private val moduleBuilder = ModuleBuilder.create()
    private val typeHolder = TypeHolder.default()
    private val varStack = VarStack()
    private var currentFunction: FunctionDataBuilder? = null
    private var returnValueAddr: Value? = null
    private var exitBlock: Label? = null
    private var stringTolabel = mutableMapOf<String, Label>()


    private fun ir(): FunctionDataBuilder {
        return currentFunction ?: throw IRCodeGenError("Function expected")
    }

    private fun getParameters(decl: DirectDeclarator): List<AnyParameter> {
        val parameters = decl.declarators[0]
        if (decl.declarators.size > 2) {
            throw IRCodeGenError("Function can have only one parameter list")
        }
        if (parameters !is FunctionDeclarator) {
            throw IRCodeGenError("Function parameters expected")
        }
        return parameters.params
    }

    private fun createType(declspec: DeclarationSpecifier, declarator: Declarator): SpecifiedType {
        val builder = visit(declspec)
        for (p in declarator.pointers) {
            val finalizedPointer = p.qualifiers
            builder.addAll(finalizedPointer)
        }

        return builder.build()
    }

    private fun createReturnType(functionNode: FunctionNode): SpecifiedType {
        val specifier = functionNode.specifier
        if (specifier !is DeclarationSpecifier) {
            throw IRCodeGenError("DeclarationSpecifier expected")
        }

        return createType(specifier, functionNode.declarator)
    }

    private fun createVar(declarator: Declarator): String {
        val identNode = declarator.declspec.decl as IdentNode
        return identNode.str.str()
    }

    private fun toIRType(type: SpecifiedType): NonTrivialType {
        return when (type.basicType) {
            CType.BOOL   -> Type.U1
            CType.CHAR   -> Type.I8
            CType.SHORT  -> Type.I16
            CType.INT    -> Type.I32
            CType.LONG   -> Type.I64
            CType.FLOAT  -> Type.F32
            CType.DOUBLE -> Type.F64
            else -> throw IRCodeGenError("Unknown type")
        }
    }

    fun visit(programNode: ProgramNode): SpecifiedType {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionNode -> visit(node)
                else -> throw IRCodeGenError("Function expected")
            }
        }
        return SpecifiedType.VOID
    }

    fun visit(functionNode: FunctionNode): SpecifiedType {
        val returnType = createReturnType(functionNode)

        val name = functionNode.declarator.declspec.decl as IdentNode
        val parameters = getParameters(functionNode.declarator.declspec)


        val types = mutableListOf<SpecifiedType>()
        val argumentNames = mutableListOf<String>()
        for (p in parameters) {
            when (p) {
                is Parameter -> {
                    val decl = p.declarator as Declarator
                    val type = createType(p.declspec, decl)
                    val arg = createVar(decl)
                    argumentNames.add(arg)
                    types.add(type)
                }
                is ParameterVarArg -> TODO()
                else -> throw IRCodeGenError("Parameter expected")
            }
        }
        val retType = toIRType(returnType)
        currentFunction = moduleBuilder.createFunction(name.str.str(), retType, types.map { toIRType(it) })

        val fn = ir()
        varStack.push()

        for (idx in argumentNames.indices) {
            val argName = argumentNames[idx]
            val arg = fn.argument(idx)
            val type = types[idx]

            val rvalueAdr = fn.alloc(arg.type())
            varStack[argName] = KeyType(type, rvalueAdr)
            fn.store(rvalueAdr, arg)
        }

        returnValueAddr = fn.alloc(retType)

        exitBlock = ir().createLabel()
        ir().switchLabel(exitBlock!!)
        val loadReturn = ir().load(retType as PrimitiveType, returnValueAddr!!)
        ir().ret(loadReturn)

        ir().switchLabel(Label.entry)
        val needSwitch = visitStatement(functionNode.body)
        if (needSwitch) {
            ir().branch(exitBlock!!)
        }

        varStack.pop()

        return returnType
    }

    fun visitStatement(statement: Statement): Boolean {
        return when (statement) {
            is CompoundStatement    -> visitCompoundStatement(statement)
            is ExprStatement        -> visitExpressionStatement(statement)
            is ReturnStatement      -> visitReturn(statement)
            is IfStatement          -> visitIf(statement)
            is WhileStatement       -> visitWhile(statement)
            is DoWhileStatement     -> visitDoWhile(statement)
            is LabeledStatement     -> visitLabeledStatement(statement)
            is GotoStatement        -> visitGoto(statement)
            else -> throw IRCodeGenError("Statement expected, but got $statement")
        }
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

    fun visitDoWhile(doWhileStatement: DoWhileStatement): Boolean {
        val bodyBlock      = ir().createLabel()
        val endBlock       = ir().createLabel()

        ir().branch(bodyBlock)
        ir().switchLabel(bodyBlock)
        visitStatement(doWhileStatement.body)

        val conditionExpr = visitExpression(doWhileStatement.condition, true)
        val condition = if (conditionExpr.type() != Type.U1) {
            val type = conditionExpr.type()
            when (type) {
                is SignedIntType     -> ir().icmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
                is UnsignedIntType   -> ir().ucmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
                is FloatingPointType -> ir().fcmp(conditionExpr, FloatPredicate.One, Constant.of(type, 0))
                is PointerType       -> ir().pcmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
                else -> throw IRCodeGenError("Unknown type")
            }
        } else {
            conditionExpr
        }

        ir().branchCond(condition, bodyBlock, endBlock)
        ir().switchLabel(endBlock)
        return true
    }

    fun visitWhile(whileStatement: WhileStatement): Boolean {
        val conditionBlock = ir().createLabel()
        val bodyBlock = ir().createLabel()
        val endBlock = ir().createLabel()

        ir().branch(conditionBlock)
        ir().switchLabel(conditionBlock)
        val conditionExpr = visitExpression(whileStatement.condition, true)
        val condition = if (conditionExpr.type() != Type.U1) {
            val type = conditionExpr.type()
            when (type) {
                is SignedIntType     -> ir().icmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
                is UnsignedIntType   -> ir().ucmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
                is FloatingPointType -> ir().fcmp(conditionExpr, FloatPredicate.One, Constant.of(type, 0))
                is PointerType       -> ir().pcmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
                else -> throw IRCodeGenError("Unknown type")
            }
        } else {
            conditionExpr
        }

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
        val conditionExpr = visitExpression(ifStatement.condition, true)
        val condition = if (conditionExpr.type() != Type.U1) {
            val type = conditionExpr.type()
            when (type) {
                is SignedIntType     -> ir().icmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
                is UnsignedIntType   -> ir().ucmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
                is FloatingPointType -> ir().fcmp(conditionExpr, FloatPredicate.One, Constant.of(type, 0))
                is PointerType       -> ir().pcmp(conditionExpr, IntPredicate.Ne, Constant.of(type, 0))
                else -> throw IRCodeGenError("Unknown type")
            }
        } else {
            conditionExpr
        }

        val thenBlock = ir().createLabel()
        val elseBlock  = ir().createLabel()

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

    fun visitExpression(expression: Expression, isRvalue: Boolean): Value {
        return when (expression) {
            is BinaryOp -> visitBinary(expression, isRvalue)
            is UnaryOp  -> visitUnary(expression, isRvalue)
            is NumNode  -> visitNumNode(expression)
            is VarNode  -> visitVarNode(expression, isRvalue)
            else -> throw IRCodeGenError("Unknown expression")
        }
    }

    fun visit(specifierType: DeclarationSpecifier): SpecifiedTypeBuilder {
        val builder = SpecifiedTypeBuilder()
        for (specifier in specifierType.specifiers) {
            when (specifier) {
                is AnyTypeNode -> {
                    builder.basicType(typeHolder.get(specifier.name()))
                }
                else -> builder.add(specifier)
            }
        }
        return builder
    }

    fun visitCompoundStatement(compoundStatement: CompoundStatement): Boolean {
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
                is Declaration -> {
                    assert(node.declarators.size == 1)
                    val decl = node.declarators[0]

                    when (decl) {
                        is Declarator -> {
                            val type = createType(node.declspec, decl)
                            val varName = createVar(decl)

                            val irType = toIRType(type)
                            val rvalueAdr = ir().alloc(irType)

                            varStack[varName] = KeyType(type, rvalueAdr)
                        }
                        is AssignmentDeclarator -> {
                            val type = createType(node.declspec, decl.rvalue)
                            val varName = createVar(decl.rvalue)

                            val irType = toIRType(type)
                            val rvalueAdr = ir().alloc(irType)
                            varStack[varName] = KeyType(type, rvalueAdr)

                            val rvalue = visitExpression(decl.lvalue, true)

                            ir().store(rvalueAdr, rvalue)
                        }
                        else -> throw IRCodeGenError("Unknown declarator, delc=$decl")
                    }
                }
                is GotoStatement -> {
                    return visitGoto(node)
                }
                is Statement -> {
                    needSwitch = visitStatement(node)
                }
                else -> throw IRCodeGenError("Statement expected")
            }
        }
        return needSwitch
    }

    fun visitBinary(binop: BinaryOp, isRvalue: Boolean): Value {
        return when (binop.type) {
            BinaryOpType.ADD -> {
                val left  = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType     = TypeConverter.interfereTypes(left, right)
                val leftConverted  = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                ir().arithmeticBinary(leftConverted, ArithmeticBinaryOp.Add,  rightConverted)
            }
            BinaryOpType.SUB -> {
                val left = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType     = TypeConverter.interfereTypes(left, right)
                val leftConverted  = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                ir().arithmeticBinary(leftConverted, ArithmeticBinaryOp.Sub,  rightConverted)
            }
            BinaryOpType.ASSIGN -> {
                val left = visitExpression(binop.left, false)
                val right = visitExpression(binop.right, true)
                ir().store(left, right)
                right //TODO
            }
            BinaryOpType.NE -> {
                val left = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType     = TypeConverter.interfereTypes(left, right)
                val leftConverted  = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                val cmp = ir().icmp(leftConverted, IntPredicate.Ne, rightConverted)
                ir().convertToType(cmp, Type.U1)
            }
            BinaryOpType.GT -> {
                val left = visitExpression(binop.left, true)
                val right = visitExpression(binop.right, true)
                val commonType     = TypeConverter.interfereTypes(left, right)
                val leftConverted  = ir().convertToType(left, commonType)
                val rightConverted = ir().convertToType(right, commonType)
                val cmp = ir().icmp(leftConverted, IntPredicate.Gt, rightConverted)
                ir().convertToType(cmp, Type.U1)
            }
            else -> throw IRCodeGenError("Unknown binary operation, op=${binop.type}")
        }
    }

    private fun visitUnary(unaryOp: UnaryOp, isRvalue: Boolean): Value {
        return when (unaryOp.type) {
            PrefixUnaryOpType.ADDRESS -> {
                visitExpression(unaryOp.primary, isRvalue)
            }
            PrefixUnaryOpType.DEREF -> {
                visitExpression(unaryOp.primary, isRvalue)
            }
            else -> throw IRCodeGenError("Unknown unary operation, op=${unaryOp.type}")
        }
    }

    fun visitNumNode(numNode: NumNode): Constant {
        return when (numNode.toLong.data) {
            in 0..255                  -> U8Value(numNode.toLong.data.toByte())
            in 0..65535                -> U16Value(numNode.toLong.data.toShort())
            in 0..4294967295           -> U32Value(numNode.toLong.data.toInt())
            in -128..127               -> I8Value(numNode.toLong.data.toByte())
            in -32768..32767           -> I16Value(numNode.toLong.data.toShort())
            in -2147483648..2147483647 -> I32Value(numNode.toLong.data.toInt())
            else -> I64Value(numNode.toLong.data.toLong())
        }
    }

    private fun visitReturn(returnStatement: ReturnStatement): Boolean {
        when (returnStatement.expr) {
            is EmptyExpression -> ir().branch(exitBlock!!)
            else -> {
                val value = visitExpression(returnStatement.expr, true)
                val realType = ir().prototype().returnType()
                val returnType = ir().convertToType(value, realType)
                ir().store(returnValueAddr!!, returnType)
                ir().branch(exitBlock!!)
            }
        }
        return false
    }

    fun visitVarNode(varNode: VarNode, isRvalue: Boolean): Value {
        val name = varNode.str.str()
        val rvalueAttr = varStack[name] ?: throw IRCodeGenError("Variable $name not found")
        if (isRvalue) {
            return ir().load(toIRType(rvalueAttr.first) as PrimitiveType, rvalueAttr.second)
        } else {
            return rvalueAttr.second
        }
    }

    companion object {
        fun apply(node: ProgramNode): Module {
            val irGen = IRGen()
            irGen.visit(node)
            val module = irGen.moduleBuilder.build()
            return module
        }
    }
}