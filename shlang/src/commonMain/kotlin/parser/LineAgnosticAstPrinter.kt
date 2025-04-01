package parser

import parser.nodes.*
import parser.nodes.visitors.NodeVisitor


//TODO refactor this class
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

    override fun visit(expression: CompoundLiteral) {
        buffer.append('(')
        expression.typeName.accept(this)
        buffer.append(')')
        visit(expression.initializerList)
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
        when (val expr = sizeOf.expr) {
            is SizeOfExpr -> expr.expr.accept(this)
            is SizeOfType -> expr.typeName.accept(this)
        }
        buffer.append(')')
    }

    override fun visit(stringNode: StringNode) {
        for (literal in stringNode.literals) {
            buffer.append(literal.str())
        }
    }

    override fun visit(assignment: CharNode) {
        buffer.append('\'')
        buffer.append(assignment.char.str())
        buffer.append('\'')
    }

    override fun visit(numNode: NumNode) {
        buffer.append(numNode.number.str())
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
            visit(it)
        }

        visit(declarator.directDeclarator)
    }

    fun visit(declaration: Declaration) {
        declaration.declspec.accept(this)
        buffer.append(' ')
        joinTo(declaration.declarators(), ", ") {
            it.accept(this)
        }

        buffer.append(';')
    }

    override fun visit(returnStatement: ReturnStatement) {
        buffer.append("return ")
        returnStatement.expr.accept(this)
        buffer.append(';')
    }

    override fun visit(ifElseStatement: IfElseStatement) {
        buffer.append("if(")
        ifElseStatement.condition.accept(this)
        buffer.append(") {")
        ifElseStatement.then.accept(this)
        buffer.append('}')
        buffer.append(" else ")
        if (ifElseStatement.elseNode !is IfElseStatement) {
            buffer.append('{')
        }
        ifElseStatement.elseNode.accept(this)
        if (ifElseStatement.elseNode !is IfElseStatement) {
            buffer.append('}')
        }
    }

    override fun visit(ifElseStatement: IfStatement) {
        buffer.append("if(")
        ifElseStatement.condition.accept(this)
        buffer.append(") {")
        ifElseStatement.then.accept(this)
        buffer.append('}')
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

        when (val init = forStatement.init) {
            is ForInitDeclaration -> visit(init.declaration)
            is ForInitExpression -> init.expression.accept(this)
            is ForInitEmpty -> buffer.append(';')
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
        caseStatement.constExpression.accept(this)
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
            when (it) {
                is CompoundStmtDeclaration -> visit(it.declaration)
                is CompoundStmtStatement -> it.statement.accept(this)
            }
        }
    }

    override fun visit(exprStatement: ExprStatement) {
        exprStatement.expr.accept(this)
    }

    fun visit(functionNode: FunctionNode) {
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
        when (val declarator = parameter.paramDeclarator) {
            is ParamDeclarator -> declarator.declarator.accept(this)
            is ParamAbstractDeclarator -> declarator.abstractDeclarator.accept(this)
            is EmptyParamDeclarator -> {}
        }
    }

    override fun visit(initDeclarator: InitDeclarator) {
        initDeclarator.declarator.accept(this)
        buffer.append(" = ")
        when (initDeclarator.rvalue) {
            is InitializerListInitializer -> visit(initDeclarator.rvalue.list)
            is ExpressionInitializer -> initDeclarator.rvalue.expr.accept(this)
        }
    }

    override fun visit(parameters: ParameterTypeList) {
        buffer.append('(')
        joinTo(parameters.params, ", ") {
            it.accept(this)
        }
        buffer.append(')')
    }

    fun visit(functionPointer: FunctionDeclarator) {
        buffer.append("(")
        functionPointer.declarator.accept(this)
        buffer.append(")")
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
        buffer.append(memberAccess.fieldName.str())
    }

    fun visit(initializerList: InitializerList) {
        buffer.append('{')
        joinTo(initializerList.initializers, ", ") {
            when (it) {
                is SingleInitializer -> visit(it)
                is DesignationInitializer -> visit(it)
            }
        }
        buffer.append('}')
    }

    fun visit(designationInitializer: DesignationInitializer) {
        visit(designationInitializer.designation)
        buffer.append(" = ")
        when (val initializer = designationInitializer.initializer) {
            is InitializerListInitializer -> visit(initializer.list)
            is ExpressionInitializer -> initializer.expr.accept(this)
        }
    }

    fun visit(singleInitializer: SingleInitializer) {
        when (val expr = singleInitializer.expr) {
            is InitializerListInitializer -> visit(expr.list)
            is ExpressionInitializer -> expr.expr.accept(this)
        }
    }

    override fun visit(builtin: BuiltinVaArg) {
        buffer.append("__builtin_va_arg")
        buffer.append('(')
        builtin.assign.accept(this)
        buffer.append(',')
        builtin.typeName.accept(this)
        buffer.append(')')
    }

    override fun visit(builtin: BuiltinVaStart) {
        buffer.append("__builtin_va_start")
        buffer.append('(')
        builtin.vaList.accept(this)
        buffer.append(',')
        builtin.param.accept(this)
        buffer.append(')')
    }

    override fun visit(builtin: BuiltinVaEnd) {
        buffer.append("__builtin_va_end")
        buffer.append('(')
        builtin.vaList.accept(this)
        buffer.append(')')
    }

    override fun visit(builtin: BuiltinVaCopy) {
        buffer.append("__builtin_va_copy")
        buffer.append('(')
        builtin.dest.accept(this)
        buffer.append(',')
        builtin.src.accept(this)
        buffer.append(')')
    }

    override fun visit(arrayDeclarator: ArrayDeclarator) {
        buffer.append('[')
        arrayDeclarator.constexpr.accept(this)
        buffer.append(']')
    }

    override fun visit(typeName: TypeName) {
        typeName.specifiers.accept(this)
        typeName.abstractDeclarator?.accept(this)
    }

    override fun visit(identifierList: IdentifierList) {
        buffer.append('(')
        joinTo(identifierList.list, ", ") {
            buffer.append(it.str())
        }
        buffer.append(')')
    }

    override fun visit(abstractDeclarator: AbstractDeclarator) {
        if (abstractDeclarator.directAbstractDeclarators == null) {
            return
        }

        joinTo(abstractDeclarator.directAbstractDeclarators, " ") {
            it.accept(this)
        }
    }

    override fun visit(specifierType: DeclarationSpecifier) {
        joinTo(specifierType.specifiers, " ") {
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

    fun visit(structField: StructField) {
        structField.declspec.accept(this)
        buffer.append(' ')
        joinTo(structField.declarators, ", ") {
            visit(it)
        }
        buffer.append(';')
    }

    override fun visit(structSpecifier: StructSpecifier) {
        buffer.append("struct ")
        buffer.append(structSpecifier.name())
        buffer.append(" {")
        joinTo(structSpecifier.fields, " ") {
            visit(it)
        }
        buffer.append('}')
    }

    override fun visit(structDeclaration: StructDeclaration) {
        buffer.append("struct ")
        buffer.append(structDeclaration.name())
    }

    override fun visit(unionSpecifier: UnionSpecifier) {
        buffer.append("union")
        buffer.append(' ')
        buffer.append(unionSpecifier.name.str())

        buffer.append(" {")
        joinTo(unionSpecifier.fields, " ") {
            visit(it)
        }
        buffer.append('}')
    }

    override fun visit(unionDeclaration: UnionDeclaration) {
        buffer.append("union ")
        buffer.append(unionDeclaration.name.str())
        buffer.append(';')
    }

    override fun visit(typeNode: TypeNode) {
        buffer.append(typeNode.name())
    }

    override fun visit(enumSpecifier: EnumSpecifier) {
        buffer.append("enum ")
        buffer.append(enumSpecifier.name())
        buffer.append(" {")
        joinTo(enumSpecifier.enumerators, ", ") {
            buffer.append(it.name())
            if (it.constExpr !is EmptyExpression) {
                buffer.append(" = ")
                it.constExpr.accept(this)
            }
        }
        buffer.append('}')
    }

    override fun visit(enumDeclaration: EnumDeclaration) {
        buffer.append("enum ")
        buffer.append(enumDeclaration.name())
        buffer.append(';')
    }

    override fun visit(functionSpecifierNode: FunctionSpecifierNode) {
        buffer.append(functionSpecifierNode.name())
    }

    override fun visit(varNode: VarNode) {
        buffer.append(varNode.name())
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess) {
        arrowMemberAccess.primary.accept(this)
        buffer.append("->")
        buffer.append(arrowMemberAccess.fieldName())
    }

    override fun visit(parameterVarArg: ParameterVarArg) {
        buffer.append("...")
    }

    private fun visitDirectDeclaratorFirstParam(param: DirectDeclaratorEntry) {
        when (param) {
            is DirectVarDeclarator -> visit(param)
            is FunctionDeclarator -> visit(param)
        }
    }

    fun visit(directDeclarator: DirectDeclarator) {
        visitDirectDeclaratorFirstParam(directDeclarator.decl)
        joinTo(directDeclarator.directDeclaratorParams, "") {
            it.accept(this)
        }
    }

    fun visit(designation: Designation) {
        joinTo(designation.designators, "") {
            when (it) {
                is ArrayDesignator -> visit(it)
                is MemberDesignator -> visit(it)
            }
        }
    }

    fun visit(arrayDesignator: ArrayDesignator) {
        buffer.append('[')
        arrayDesignator.constExpression.accept(this)
        buffer.append(']')
    }

    fun visit(memberDesignator: MemberDesignator) {
        buffer.append('.')
        buffer.append(memberDesignator.name())
    }

    private fun visit(varDecl: DirectVarDeclarator) {
        buffer.append(varDecl.name())
    }

    override fun visit(emptyExpression: EmptyExpression) {

    }

    override fun visit(typeQualifier: TypeQualifierNode) {
        buffer.append(typeQualifier.name())
    }

    override fun visit(storageClassSpecifier: StorageClassSpecifier) {
        buffer.append(storageClassSpecifier.name())
    }

    fun visit(structDeclarator: StructDeclarator) {
        when (val item = structDeclarator.declarator) {
            is EmptyStructDeclaratorItem -> {}
            is StructDeclaratorItem -> visit(item.expr)
        }

        if (structDeclarator.expr !is EmptyExpression) {
            buffer.append(':')
            structDeclarator.expr.accept(this)
        }
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

    fun visit(nodePointer: NodePointer) {
        buffer.append('*')
        for (qualifier in nodePointer.qualifiers) {
            buffer.append(qualifier)
            buffer.append(' ')
        }
    }

    companion object {
        fun print(type: TypeSpecifier): String {
            val astPrinter = LineAgnosticAstPrinter()
            type.accept(astPrinter)
            return astPrinter.buffer.toString()
        }

        fun print(stmt: Statement): String {
            val astPrinter = LineAgnosticAstPrinter()
            stmt.accept(astPrinter)
            return astPrinter.buffer.toString()
        }

        fun print(expr: Expression): String {
            val astPrinter = LineAgnosticAstPrinter()
            expr.accept(astPrinter)
            return astPrinter.buffer.toString()
        }

        fun print(decl: AnyDeclarator): String {
            val astPrinter = LineAgnosticAstPrinter()
            decl.accept(astPrinter)
            return astPrinter.buffer.toString()
        }

        fun print(functionNode: FunctionNode): String {
            val astPrinter = LineAgnosticAstPrinter()
            astPrinter.visit(functionNode)
            return astPrinter.buffer.toString()
        }

        fun print(entry: InitializerListEntry) : String {
            val astPrinter = LineAgnosticAstPrinter()
            when (entry) {
                is SingleInitializer -> astPrinter.visit(entry)
                is DesignationInitializer -> astPrinter.visit(entry)
            }
            return astPrinter.buffer.toString()
        }

        fun print(initializerList: InitializerList): String {
            val astPrinter = LineAgnosticAstPrinter()
            astPrinter.visit(initializerList)
            return astPrinter.buffer.toString()
        }

        fun print(abstractDeclarator: AbstractDeclarator): String {
            val astPrinter = LineAgnosticAstPrinter()
            abstractDeclarator.accept(astPrinter)
            return astPrinter.buffer.toString()
        }

        fun print(declaration: Declaration): String {
            val astPrinter = LineAgnosticAstPrinter()
            astPrinter.visit(declaration)
            return astPrinter.buffer.toString()
        }

        fun print(programNode: ProgramNode): String {
            val astPrinter = LineAgnosticAstPrinter()
            for ((idx, node) in programNode.nodes.withIndex()) {
                when (node) {
                    is FunctionDeclarationNode -> astPrinter.visit(node.function)
                    is GlobalDeclaration -> astPrinter.visit(node.declaration)
                }
                if (idx != programNode.nodes.size - 1) {
                    astPrinter.buffer.append(' ')
                }
            }
            return astPrinter.buffer.toString()
        }
    }
}