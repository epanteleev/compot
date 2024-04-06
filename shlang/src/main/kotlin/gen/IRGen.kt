package gen

import ir.*
import ir.instruction.ArithmeticBinaryOp
import types.*
import ir.module.Module
import ir.module.builder.impl.FunctionDataBuilder
import ir.module.builder.impl.ModuleBuilder
import ir.types.*
import parser.nodes.*
import java.lang.Exception


data class IRCodeGenError(override val message: String): Exception(message)

class IRGen private constructor(): NodeVisitor<Any> {
    private val moduleBuilder = ModuleBuilder.create()
    private val typeHolder = TypeHolder.default()
    private val varStack = VarStack()
    private var currentFunction: FunctionDataBuilder? = null

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
            val finalizedPointer = visit(p)
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

    private fun interfereTypes(a: Value, b: Value): Type {
        val aType = a.type()
        val bType = b.type()
        if (aType == bType) {
            return aType
        }

        return when (aType) {
            Type.I8 -> {
                when (bType) {
                    Type.I16 -> Type.I16
                    Type.I32 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.I8
                    Type.U16 -> Type.I16
                    Type.U32 -> Type.I32
                    Type.U64 -> Type.I64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.I16 -> {
                when (bType) {
                    Type.I8  -> Type.I16
                    Type.I32 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.I16
                    Type.U16 -> Type.I16
                    Type.U32 -> Type.I32
                    Type.U64 -> Type.I64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.I32 -> {
                when (bType) {
                    Type.I8  -> Type.I32
                    Type.I16 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.I32
                    Type.U16 -> Type.I32
                    Type.U32 -> Type.I32
                    Type.U64 -> Type.I64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.I64 -> {
                when (bType) {
                    Type.I8  -> Type.I64
                    Type.I16 -> Type.I64
                    Type.I32 -> Type.I64
                    Type.U8  -> Type.I64
                    Type.U16 -> Type.I64
                    Type.U32 -> Type.I64
                    Type.U64 -> Type.I64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.U8 -> {
                when (bType) {
                    Type.I8  -> Type.I8
                    Type.I16 -> Type.I16
                    Type.I32 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U16 -> Type.U16
                    Type.U32 -> Type.U32
                    Type.U64 -> Type.U64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.U16 -> {
                when (bType) {
                    Type.I8  -> Type.I16
                    Type.I16 -> Type.I16
                    Type.I32 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.U16
                    Type.U32 -> Type.U32
                    Type.U64 -> Type.U64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.U32 -> {
                when (bType) {
                    Type.I8  -> Type.I32
                    Type.I16 -> Type.I32
                    Type.I32 -> Type.I32
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.U32
                    Type.U16 -> Type.U32
                    Type.U64 -> Type.U64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.U64 -> {
                when (bType) {
                    Type.I8  -> Type.I64
                    Type.I16 -> Type.I64
                    Type.I32 -> Type.I64
                    Type.I64 -> Type.I64
                    Type.U8  -> Type.U64
                    Type.U16 -> Type.U64
                    Type.U32 -> Type.U64
                    Type.F32 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.F32 -> {
                when (bType) {
                    Type.I8  -> Type.F32
                    Type.I16 -> Type.F32
                    Type.I32 -> Type.F32
                    Type.I64 -> Type.F32
                    Type.U8  -> Type.F32
                    Type.U16 -> Type.F32
                    Type.U32 -> Type.F32
                    Type.U64 -> Type.F32
                    Type.F64 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            Type.F64 -> {
                when (bType) {
                    Type.I8  -> Type.F64
                    Type.I16 -> Type.F64
                    Type.I32 -> Type.F64
                    Type.I64 -> Type.F64
                    Type.U8  -> Type.F64
                    Type.U16 -> Type.F64
                    Type.U32 -> Type.F64
                    Type.U64 -> Type.F64
                    Type.F32 -> Type.F64
                    else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
                }
            }
            else -> throw IRCodeGenError("Types $aType and $bType are not compatible")
        }
    }

    private fun convertToType(value: Value, type: Type): Value {
        if (value.type() == type) {
            return value
        }

        return when (type) {
            Type.I8 -> {
                type as SignedIntType
                when (value.type()) {
                    Type.I16 -> ir().trunc(value, type)
                    Type.I32 -> ir().trunc(value, type)
                    Type.I64 -> ir().trunc(value, type)
                    Type.U8  -> ir().trunc(value, type)
                    Type.U16 -> ir().trunc(value, type)
                    Type.U32 -> ir().trunc(value, type)
                    Type.U64 -> ir().trunc(value, type)
                    Type.F32 -> ir().fptosi(value, type)
                    Type.F64 -> ir().fptosi(value, type)
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.I16 -> {
                type as SignedIntType
                when (value.type()) {
                    Type.I8  -> ir().sext(value, type)
                    Type.I32 -> ir().trunc(value, type)
                    Type.I64 -> ir().trunc(value, type)
                    Type.U8  -> ir().sext(value, type)
                    Type.U16 -> ir().bitcast(value, type)
                    Type.U32 -> ir().trunc(value, type)
                    Type.U64 -> ir().trunc(value, type)
                    Type.F32 -> ir().fptosi(value, type)
                    Type.F64 -> ir().fptosi(value, type)
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.I32 -> {
                type as SignedIntType
                when (value.type()) {
                    Type.I8 -> ir().sext(value, type)
                    Type.I16 -> ir().sext(value, type)
                    Type.I64 -> ir().trunc(value, type)
                    Type.U8  -> {
                        val tmp = ir().sext(value, Type.I32)
                        ir().trunc(tmp, type)
                    }
                    Type.U16 -> {
                        val tmp = ir().zext(value, Type.U32)
                        ir().trunc(tmp, type)
                    }
                    Type.U32 -> ir().bitcast(value, type)
                    Type.U64 -> ir().trunc(value, type)
                    Type.F32 -> ir().fptosi(value, type)
                    Type.F64 -> ir().fptosi(value, type)
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.I64 -> {
                type as SignedIntType
                when (value.type()) {
                    Type.I8 -> ir().sext(value, type)
                    Type.I16 -> ir().sext(value, type)
                    Type.I32 -> ir().sext(value, type)
                    Type.U8  -> {
                        val tmp = ir().sext(value, Type.I64)
                        ir().trunc(tmp, type)
                    }
                    Type.U16 -> {
                        val tmp = ir().zext(value, Type.U64)
                        ir().trunc(tmp, type)
                    }
                    Type.U32 -> {
                        val tmp = ir().zext(value, Type.U64)
                        ir().trunc(tmp, type)
                    }
                    Type.U64 -> ir().bitcast(value, type)
                    Type.F32 -> ir().fptosi(value, type)
                    Type.F64 -> ir().fptosi(value, type)
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.U8 -> {
                type as UnsignedIntType
                when (value.type()) {
                    Type.I8  -> ir().bitcast(value, type)
                    Type.I16 -> ir().trunc(value, type)
                    Type.I32 -> ir().trunc(value, type)
                    Type.I64 -> ir().trunc(value, type)
                    Type.U16 -> ir().trunc(value, type)
                    Type.U32 -> ir().trunc(value, type)
                    Type.U64 -> ir().trunc(value, type)
                    Type.F32 -> {
                        val tmp = ir().fptosi(value, Type.I32)
                        ir().trunc(tmp, type)
                    }
                    Type.F64 -> {
                        val tmp = ir().fptosi(value, Type.I64)
                        ir().trunc(tmp, type)
                    }
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.U16 -> {
                type as UnsignedIntType
                when (value.type()) {
                    Type.I8  -> ir().trunc(value, type)
                    Type.I16 -> ir().bitcast(value, type)
                    Type.I32 -> ir().trunc(value, type)
                    Type.I64 -> ir().trunc(value, type)
                    Type.U8  -> ir().trunc(value, type)
                    Type.U32 -> ir().trunc(value, type)
                    Type.U64 -> ir().trunc(value, type)
                    Type.F32 -> {
                        val tmp = ir().fptosi(value, Type.I32)
                        ir().trunc(tmp, type)
                    }
                    Type.F64 -> {
                        val tmp = ir().fptosi(value, Type.I64)
                        ir().trunc(tmp, type)
                    }
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }

            Type.U32 -> {
                type as UnsignedIntType
                when (value.type()) {
                    Type.I8  -> ir().trunc(value, type)
                    Type.I16 -> ir().trunc(value, type)
                    Type.I32 -> ir().bitcast(value, type)
                    Type.I64 -> ir().trunc(value, type)
                    Type.U8  -> ir().trunc(value, type)
                    Type.U16 -> ir().trunc(value, type)
                    Type.U64 -> ir().trunc(value, type)
                    Type.F32 -> {
                        val tmp = ir().fptosi(value, Type.I32)
                        ir().trunc(tmp, type)
                    }
                    Type.F64 -> {
                        val tmp = ir().fptosi(value, Type.I64)
                        ir().trunc(tmp, type)
                    }
                    else -> throw IRCodeGenError("Cannot convert $value to $type")
                }
            }
            else -> throw IRCodeGenError("Cannot convert $value to $type")
        }
    }

    override fun visit(programNode: ProgramNode): SpecifiedType {
        for (node in programNode.nodes) {
            node.accept(this)
        }
        return SpecifiedType.VOID
    }

    override fun visit(functionNode: FunctionNode): SpecifiedType {
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

        functionNode.body.accept(this)
        varStack.pop()

        return returnType
    }

    override fun visit(specifierType: DeclarationSpecifier): SpecifiedTypeBuilder {
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

    override fun visit(compoundStatement: CompoundStatement): SpecifiedType {
        for (node in compoundStatement.statements) {
            when (node) {
                is Declaration -> {
                    assert(node.declarators.size == 1)
                    val decl = node.declarators[0] as Declarator

                    val type = createType(node.declspec, decl)
                    val varName = createVar(decl)

                    val irType = toIRType(type)
                    val rvalueAdr = ir().alloc(irType)

                    varStack[varName] = KeyType(type, rvalueAdr)
                }
                is Statement -> node.accept(this)
                else -> throw IRCodeGenError("Statement expected")
            }
        }
        return SpecifiedType.VOID
    }

    override fun visit(node: DummyNode): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(binop: BinaryOp): Value {
        val left = binop.left.accept(this) as Value
        val right = binop.right.accept(this) as Value

        val commonType     = interfereTypes(left, right)
        val leftConverted  = convertToType(left, commonType)
        val rightConverted = convertToType(right, commonType)

        return when (binop.type) {
            BinaryOpType.ADD -> ir().arithmeticBinary(leftConverted, ArithmeticBinaryOp.Add,  rightConverted)
            else -> throw IRCodeGenError("Unknown binary operation")
        }
    }

    override fun visit(pointer: NodePointer): List<TypeProperty> {
        TODO("Not yet implemented")
    }

    override fun visit(node: IdentNode): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(cast: Cast): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(arrayAccess: ArrayAccess): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(unaryOp: UnaryOp): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(sizeOf: SizeOf): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(stringNode: StringNode): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(numNode: NumNode): Constant {
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

    override fun visit(switchStatement: SwitchStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(declarator: Declarator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(declaration: Declaration): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(returnStatement: ReturnStatement): SpecifiedType {
        when (returnStatement.expr) {
            is EmptyExpression -> {
                ir().retVoid()
            }
            else -> {
                val value = returnStatement.expr.accept(this) as Value
                val realType = ir().prototype().returnType()
                val returnType = convertToType(value, realType)
                ir().ret(returnType)
            }
        }
        return SpecifiedType.VOID
    }

    override fun visit(ifStatement: IfStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(whileStatement: WhileStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(forStatement: ForStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(doWhileStatement: DoWhileStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(caseStatement: CaseStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(defaultStatement: DefaultStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(breakStatement: BreakStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(continueStatement: ContinueStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(gotoStatement: GotoStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(labeledStatement: LabeledStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(emptyStatement: EmptyStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(exprStatement: ExprStatement): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(parameter: Parameter): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(declspec: Declspec): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(functionParams: FunctionParams): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(assignmentDeclarator: AssignmentDeclarator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(rValueDeclarator: RValueDeclarator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(functionDeclarator: FunctionDeclarator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(functionPointerDeclarator: FunctionPointerDeclarator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(functionPointerParamDeclarator: FunctionPointerParamDeclarator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(conditional: Conditional): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(memberAccess: MemberAccess): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(initializerList: InitializerList): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(compoundLiteral: CompoundLiteral): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(arrayDeclarator: ArrayDeclarator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(typeName: TypeName): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(directFunctionDeclarator: DirectFunctionDeclarator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(directArrayDeclarator: DirectArrayDeclarator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(abstractDeclarator: AbstractDeclarator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(functionCall: FunctionCall): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(structField: StructField): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(structSpecifier: StructSpecifier): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(structDeclaration: StructDeclaration): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(unionSpecifier: UnionSpecifier): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(unionDeclaration: UnionDeclaration): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(typeNode: TypeNode): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(enumSpecifier: EnumSpecifier): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(enumDeclaration: EnumDeclaration): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(enumerator: Enumerator): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(varNode: VarNode): Value {
        val name = varNode.str.str()
        val rvalueAttr = varStack[name] ?: throw IRCodeGenError("Variable $name not found")
        return ir().load(toIRType(rvalueAttr.first) as PrimitiveType, rvalueAttr.second)
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(parameterVarArg: ParameterVarArg): SpecifiedType {
        TODO("Not yet implemented")
    }

    override fun visit(directDeclarator: DirectDeclarator): Any {
        TODO("Not yet implemented")
    }

    override fun visit(emptyExpression: EmptyExpression): Any {
        TODO("Not yet implemented")
    }

    companion object {
        fun apply(node: ProgramNode): Module {
            val irGen = IRGen()
            node.accept(irGen)
            return irGen.moduleBuilder.build()
        }
    }
}