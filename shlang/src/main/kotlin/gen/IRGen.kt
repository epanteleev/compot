package gen

import ir.Value
import types.*
import ir.module.Module
import ir.module.builder.impl.FunctionDataBuilder
import ir.module.builder.impl.ModuleBuilder
import ir.types.NonTrivialType
import ir.types.PrimitiveType
import ir.types.TrivialType
import ir.types.Type
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

    override fun visit(programNode: ProgramNode): SpecifiedType {
        for (node in programNode.nodes) {
            node.accept(this)
        }
        return SpecifiedType.VOID
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
        currentFunction = moduleBuilder.createFunction(name.str.str(), toIRType(returnType), types.map { toIRType(it) })

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

    override fun visit(binop: BinaryOp): SpecifiedType {
        TODO("Not yet implemented")
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

    override fun visit(numNode: NumNode): SpecifiedType {
        TODO("Not yet implemented")
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

//        when (returnStatement.expr) {
//            is EmptyExpr -> return ir().retVoid()
//
//        }
        val type = returnStatement.expr.accept(this) as Value

        when (type.type()) {
            is NonTrivialType -> ir().ret(type)
            is TrivialType    -> ir().retVoid()
            else -> throw IRCodeGenError("Primitive type expected")
        }
        TODO()
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

    override fun visit(varNode: VarNode): SpecifiedType {
        TODO("Not yet implemented")
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