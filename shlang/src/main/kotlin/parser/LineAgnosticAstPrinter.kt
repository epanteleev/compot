package parser

import parser.nodes.*
import tokenizer.Ident
import types.PointerQualifier


class LineAgnosticAstPrinter: NodeVisitor<Unit> {
    private val buffer = StringBuilder()

    fun <T> joinTo(list: Iterable<T>, separator: CharSequence = ", ", prefix: CharSequence = "", transform: ((T) -> Unit)) {
        var count = 0
        buffer.append(prefix)
        for (element in list) {
            if (++count > 1) {
                buffer.append(separator)
            }
            transform(element)
            count += 1
        }
    }

    override fun visit(node: DummyNode) {}

    override fun visit(node: IdentNode) {
        buffer.append(node.str.str())
    }

    override fun visit(cast: Cast) {
        buffer.append('(')
        cast.typeName.accept(this)
        buffer.append(')')
        buffer.append(' ')
        cast.cast.accept(this)
    }

    override fun visit(arrayAccess: ArrayAccess) {
        arrayAccess.primary.accept(this)
        buffer.append('[')
        arrayAccess.expr.accept(this)
        buffer.append(']')
    }

    override fun visit(unaryOp: UnaryOp) {
        if (unaryOp.opType is PrefixUnaryOpType) {
            buffer.append(unaryOp.opType)
        }
        unaryOp.primary.accept(this)
        if (unaryOp.opType is PostfixUnaryOpType) {
            buffer.append(unaryOp.opType)
        }
    }

    override fun visit(sizeOf: SizeOf) {
        buffer.append("sizeof(")
        sizeOf.expr.accept(this)
        buffer.append(')')
    }

    override fun visit(stringNode: StringNode) {
        buffer.append(stringNode.str.str())
    }

    override fun visit(numNode: NumNode) {
        buffer.append(numNode.toLong.str())
    }

    override fun visit(switchStatement: SwitchStatement) {
        buffer.append("switch(")
        switchStatement.condition.accept(this)
        buffer.append(") {")
        switchStatement.body.accept(this)
        buffer.append('}')
    }

    override fun visit(declarator: Declarator) {
        joinTo(declarator.pointers, "") {
            it.accept(this)
        }

        declarator.directDeclarator.accept(this)
    }

    override fun visit(declaration: Declaration) {
        declaration.declspec.accept(this)
        buffer.append(' ')
        joinTo(declaration.declarators, ", ") {
            it.accept(this)
        }

        buffer.append(';')
    }

    override fun visit(returnStatement: ReturnStatement) {
        buffer.append("return ")
        returnStatement.expr.accept(this)
        buffer.append(';')
    }

    override fun visit(ifStatement: IfStatement) {
        buffer.append("if(")
        ifStatement.condition.accept(this)
        buffer.append(") {")
        ifStatement.then.accept(this)
        buffer.append('}')
        if (ifStatement.elseNode !is EmptyStatement) {
            buffer.append(" else ")
            if (ifStatement.elseNode !is IfStatement) {
                buffer.append('{')
            }

            ifStatement.elseNode.accept(this)

            if (ifStatement.elseNode !is IfStatement) {
                buffer.append('}')
            }
        }
    }

    override fun visit(whileStatement: WhileStatement) {
        buffer.append("while(")
        whileStatement.condition.accept(this)
        buffer.append(") {")
        whileStatement.body.accept(this)
        buffer.append('}')
    }

    override fun visit(forStatement: ForStatement) {
        buffer.append("for(")

        forStatement.init.accept(this)
        if (forStatement.init is EmptyStatement) {
            buffer.append(';')
        }
        forStatement.condition.accept(this)
        buffer.append(';')
        forStatement.update.accept(this)
        buffer.append(") {")
        forStatement.body.accept(this)
        buffer.append('}')
    }

    override fun visit(doWhileStatement: DoWhileStatement) {
        buffer.append("do {")
        doWhileStatement.body.accept(this)
        buffer.append("} while(")
        doWhileStatement.condition.accept(this)
        buffer.append(");")
    }

    override fun visit(caseStatement: CaseStatement) {
        buffer.append("case ")
        caseStatement.expr.accept(this)
        buffer.append(": ")
        caseStatement.stmt.accept(this)
    }

    override fun visit(defaultStatement: DefaultStatement) {
        buffer.append("default: ")
        defaultStatement.stmt.accept(this)
    }

    override fun visit(breakStatement: BreakStatement) {
        buffer.append("break;")
    }

    override fun visit(continueStatement: ContinueStatement) {
        buffer.append("continue;")
    }

    override fun visit(gotoStatement: GotoStatement) {
        buffer.append("goto ")
        buffer.append(gotoStatement.id.str())
        buffer.append(';')
    }
    override fun visit(labeledStatement: LabeledStatement) {
        buffer.append(labeledStatement.label.str())
        buffer.append(": ")
        labeledStatement.stmt.accept(this)
    }

    override fun visit(compoundStatement: CompoundStatement) {
        joinTo(compoundStatement.statements, " ") {
            it.accept(this)
        }
    }

    override fun visit(exprStatement: ExprStatement) {
        exprStatement.expr.accept(this)
    }

    override fun visit(functionNode: FunctionNode) {
        functionNode.specifier.accept(this)
        buffer.append(' ')
        functionNode.declarator.accept(this)
        buffer.append(" {")
        functionNode.body.accept(this)
        buffer.append('}')
    }

    override fun visit(parameter: Parameter) {
        parameter.declspec.accept(this)
        buffer.append(' ')
        parameter.declarator.accept(this)
    }

    override fun visit(declspec: Declspec) {
        buffer.append(declspec.ident.str())
    }

    override fun visit(functionParams: FunctionParams) {
        buffer.append('(')
        joinTo(functionParams.params, ", ") {
            it.accept(this)
        }
        buffer.append(')')
    }

    override fun visit(assignmentDeclarator: AssignmentDeclarator) {
        assignmentDeclarator.rvalue.accept(this)
        buffer.append(" = ")
        assignmentDeclarator.lvalue.accept(this)
    }

    override fun visit(functionDeclarator: FunctionDeclarator) {
        buffer.append('(')
        joinTo(functionDeclarator.params, ", ") {
            it.accept(this)
        }
        buffer.append(')')
    }

    override fun visit(functionPointerDeclarator: FunctionPointerDeclarator) {
        buffer.append("(")
        joinTo(functionPointerDeclarator.declarator, " ") {
            it.accept(this)
        }
        buffer.append(")")
    }

    override fun visit(functionPointerParamDeclarator: FunctionPointerParamDeclarator) {
        buffer.append("(")
        functionPointerParamDeclarator.declarator.accept(this)
        buffer.append(")")
        functionPointerParamDeclarator.params.accept(this)
    }

    override fun visit(conditional: Conditional) {
        conditional.cond.accept(this)
        buffer.append("? ")
        conditional.eTrue.accept(this)
        buffer.append(" : ")
        conditional.eFalse.accept(this)
    }

    override fun visit(memberAccess: MemberAccess) {
        memberAccess.primary.accept(this)
        buffer.append('.')
        buffer.append(memberAccess.ident.str())
    }

    override fun visit(initializerList: InitializerList) {
        buffer.append('{')
        joinTo(initializerList.initializers, ", ") {
            it.accept(this)
        }
        buffer.append('}')
    }

    override fun visit(compoundLiteral: CompoundLiteral) {
        buffer.append('(')
        buffer.append(compoundLiteral.typeName)
        buffer.append(')')
        compoundLiteral.initializerList.accept(this)
    }

    override fun visit(arrayDeclarator: ArrayDeclarator) {
        buffer.append('[')
        arrayDeclarator.constexpr.accept(this)
        buffer.append(']')
    }

    override fun visit(typeName: TypeName) {
        joinTo(typeName.specifiers, " ") {
            if (it is Node) {
                it.accept(this)
            } else {
                buffer.append(it)
            }
        }
        typeName.abstractDecl.accept(this)
    }

    override fun visit(directFunctionDeclarator: DirectFunctionDeclarator) {
        buffer.append('(')
        joinTo(directFunctionDeclarator.parameters, ", ") {
            it.accept(this)
        }
        buffer.append(')')
    }

    override fun visit(directArrayDeclarator: DirectArrayDeclarator) {
        buffer.append('[')
        directArrayDeclarator.size.accept(this)
        buffer.append(']')
    }

    override fun visit(abstractDeclarator: AbstractDeclarator) {
        joinTo(abstractDeclarator.directAbstractDeclarator, " ") {
            it.accept(this)
        }
    }

    override fun visit(specifierType: DeclarationSpecifier) {
        joinTo(specifierType.specifiers, " ") {
            if (it is Node) {
                it.accept(this)
            } else {
                buffer.append(it)
            }
        }
    }

    override fun visit(programNode: ProgramNode) {
        joinTo(programNode.nodes, " ") {
            it.accept(this)
        }
    }

    override fun visit(functionCall: FunctionCall) {
        functionCall.primary.accept(this)
        buffer.append('(')
        joinTo(functionCall.args, ", ") {
            it.accept(this)
        }
        buffer.append(')')
    }

    override fun visit(structField: StructField) {
        joinTo(structField.declspec, " ") {
            if (it is Node) {
                it.accept(this)
            } else {
                buffer.append(it)
            }
        }
        buffer.append(' ')
        joinTo(structField.declarators, ", ") {
            it.accept(this)
        }
        buffer.append(';')
    }

    override fun visit(structSpecifier: StructSpecifier) {
        buffer.append("struct ")
        buffer.append(structSpecifier.ident.str())
        buffer.append(" {")
        joinTo(structSpecifier.fields, " ") {
            it.accept(this)
        }
        buffer.append('}')
    }

    override fun visit(structDeclaration: StructDeclaration) {
        buffer.append("struct ")
        buffer.append(structDeclaration.name.str())
    }

    override fun visit(unionSpecifier: UnionSpecifier) {
        buffer.append("union")
        if (unionSpecifier.ident != Ident.UNKNOWN) {
            buffer.append(' ')
            buffer.append(unionSpecifier.ident.str())
        }

        buffer.append(" {")
        joinTo(unionSpecifier.fields, " ") {
            it.accept(this)
        }
        buffer.append('}')
    }

    override fun visit(unionDeclaration: UnionDeclaration) {
        buffer.append("union ")
        buffer.append(unionDeclaration.name.str())
        buffer.append(';')
    }

    override fun visit(typeNode: TypeNode) {
        buffer.append(typeNode.ident.str())
    }

    override fun visit(enumSpecifier: EnumSpecifier) {
        buffer.append("enum ")
        buffer.append(enumSpecifier.ident.str())
        buffer.append(" {")
        joinTo(enumSpecifier.enumerators, ", ") {
            it.accept(this)
        }
        buffer.append('}')
    }

    override fun visit(enumDeclaration: EnumDeclaration) {
        buffer.append("enum ")
        buffer.append(enumDeclaration.name.str())
        buffer.append(';')
    }

    override fun visit(enumerator: Enumerator) {
        buffer.append(enumerator.ident.str())
        if (enumerator.expr !is DummyNode) {
            buffer.append(" = ")
            enumerator.expr.accept(this)
        }
    }

    override fun visit(varNode: VarNode) {
        buffer.append(varNode.str.str())
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess) {
        arrowMemberAccess.primary.accept(this)
        buffer.append("->")
        buffer.append(arrowMemberAccess.ident.str())
    }

    override fun visit(parameterVarArg: ParameterVarArg) {
        buffer.append("...")
    }

    override fun visit(directDeclarator: DirectDeclarator) {
        directDeclarator.decl.accept(this)
        joinTo(directDeclarator.declarators, "") {
            it.accept(this)
        }
    }

    override fun visit(emptyExpression: EmptyExpression) {

    }

    override fun visit(emptyDeclarator: EmptyDeclarator) {

    }

    override fun visit(varDeclarator: VarDeclarator) {
        buffer.append(varDeclarator.ident.str())
    }

    override fun visit(typeQualifier: TypeQualifier) {
        buffer.append(typeQualifier.name())
    }

    override fun visit(storageClassSpecifier: StorageClassSpecifier) {
        buffer.append(storageClassSpecifier.name())
    }

    override fun visit(emptyStatement: EmptyStatement) {
    }

    override fun visit(binop: BinaryOp) {
        binop.left.accept(this)
        buffer.append(' ').append(binop.opType).append(' ')
        binop.right.accept(this)
        if (binop.opType == BinaryOpType.ASSIGN) {
            buffer.append(';')
        }
    }

    override fun visit(pointer: NodePointer) {
        buffer.append('*')
        for (qualifier in pointer.qualifiers) {
            if (qualifier == PointerQualifier.EMPTY) {
                continue
            }
            buffer.append(qualifier)
            buffer.append(' ')
        }
    }

    companion object {
        fun print(expr: Node): String {
            val astPrinter = LineAgnosticAstPrinter()
            expr.accept(astPrinter)
            return astPrinter.buffer.toString()
        }
    }
}