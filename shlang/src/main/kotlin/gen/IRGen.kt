package gen

import types.*
import ir.module.Module
import ir.module.builder.impl.ModuleBuilder
import parser.nodes.*


class IRGen private constructor(): NodeVisitor<Any> {
    private val moduleBuilder = ModuleBuilder.create()
    private val typeHolder = TypeHolder.default()

    override fun visit(programNode: ProgramNode): SpecifiedType {
        for (node in programNode.nodes) {
            node.accept(this)
        }
        return SpecifiedType.VOID
    }

    override fun visit(functionNode: FunctionNode): SpecifiedType {
        val returnType = functionNode.specifier.accept(this) as SpecifiedTypeBuilder

        for (p in functionNode.declarator.pointers) {
            val finalizedPointer = visit(p)
            returnType.addAll(finalizedPointer)
        }

        val name = functionNode.declarator.declspec.decl as IdentNode
        //val params = functionNode.declarator.params
        //moduleBuilder.createFunction(name.str.str(), )

        return returnType.build()
    }

    override fun visit(specifierType: DeclarationSpecifier): SpecifiedTypeBuilder {
        val builder = SpecifiedTypeBuilder()
        for (specifier in specifierType.specifiers) {
            when (specifier) {
                is AnyTypeNode -> {
                    builder.add(typeHolder.get(specifier.name()))
                }
                else -> builder.add(specifier)
            }
        }
        return builder
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
        TODO("Not yet implemented")
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

    override fun visit(compoundStatement: CompoundStatement): SpecifiedType {
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


    companion object {
        fun apply(node: ProgramNode): Module {
            val irGen = IRGen()
            node.accept(irGen)
            return irGen.moduleBuilder.build()
        }
    }
}