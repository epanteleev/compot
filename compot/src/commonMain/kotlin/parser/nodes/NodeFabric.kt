package parser.nodes

import tokenizer.Position
import tokenizer.tokens.CharLiteral
import tokenizer.tokens.Identifier
import tokenizer.tokens.Keyword
import tokenizer.tokens.PPNumber
import tokenizer.tokens.StringLiteral

class NodeFabric {
    private var declCounter = 0
    private var expressionCounter = 0
    private var typeSpecCounter = 0
    private var statementCounter = 0

    private fun incExpressionCounter(): Int {
        val current = expressionCounter
        expressionCounter++
        return current
    }

    private fun incTypeSpecCounter(): Int {
        val current = typeSpecCounter
        typeSpecCounter++
        return current
    }

    private fun incStatementCounter(): Int {
        val current = statementCounter
        statementCounter++
        return current
    }

    fun newIdentifier(ident: Identifier): IdentNode {
        return IdentNode(ident)
    }

    // New declarator factories
    fun newDeclarator(directDeclarator: DirectDeclarator, pointers: List<NodePointer>): Declarator {
        return Declarator(declCounter++, directDeclarator, pointers)
    }

    fun newInitDeclarator(declarator: Declarator, initializer: Initializer): InitDeclarator {
        return InitDeclarator(declCounter++, declarator, initializer)
    }

    //Expression factories
    fun newBinaryOp(left: Expression, right: Expression, op: BinaryOpType): BinaryOp {
        return BinaryOp(incExpressionCounter(), left, right, op)
    }

    fun newUnaryOp(expr: Expression, op: UnaryOpType): UnaryOp {
        return UnaryOp(incExpressionCounter(), expr, op)
    }

    fun newFunctionCall(func: Expression, args: List<Expression>): FunctionCall {
        return FunctionCall(incExpressionCounter(), func, args)
    }

    fun newArrayAccess(array: Expression, index: Expression): ArrayAccess {
        return ArrayAccess(incExpressionCounter(), array, index)
    }

    fun newSizeOf(expr: SizeOfParam): SizeOf {
        return SizeOf(incExpressionCounter(), expr)
    }

    fun newCast(typeName: TypeName, cast: Expression): Cast {
        return Cast(incExpressionCounter(), typeName, cast)
    }

    fun newBuiltinVaArg(assign: Expression, typeName: TypeName): BuiltinVaArg {
        return BuiltinVaArg(incExpressionCounter(), assign, typeName)
    }

    fun newBuiltinVaStart(vaList: Expression, func: Expression): BuiltinVaStart {
        return BuiltinVaStart(incExpressionCounter(), vaList, func)
    }

    fun newBuiltinVaEnd(vaList: Expression): BuiltinVaEnd {
        return BuiltinVaEnd(incExpressionCounter(), vaList)
    }

    fun newBuiltinVaCopy(dest: Expression, src: Expression): BuiltinVaCopy {
        return BuiltinVaCopy(incExpressionCounter(), dest, src)
    }

    fun newMemberAccess(struct: Expression, member: Identifier): MemberAccess {
        return MemberAccess(incExpressionCounter(), struct, member)
    }

    fun newNumNode(number: PPNumber): NumNode {
        return NumNode(incExpressionCounter(), number)
    }

    fun newCharNode(char: CharLiteral): CharNode {
        return CharNode(incExpressionCounter(), char)
    }

    fun newStringNode(literals: List<StringLiteral>): StringNode {
        return StringNode(incExpressionCounter(), literals)
    }

    fun newVarNode(ident: Identifier): VarNode {
        return VarNode(incExpressionCounter(), ident)
    }

    fun newArrowMemberAccess(struct: Expression, member: Identifier): ArrowMemberAccess {
        return ArrowMemberAccess(incExpressionCounter(), struct, member)
    }

    fun newCompoundLiteral(typeName: TypeName, initializerList: InitializerList): CompoundLiteral {
        return CompoundLiteral(incExpressionCounter(), typeName, initializerList)
    }

    fun newEmptyExpression(position: Position): EmptyExpression {
        return EmptyExpression(incExpressionCounter(), position)
    }

    fun newConditional(condition: Expression, trueExpr: Expression, falseExpr: Expression): Conditional {
        return Conditional(incExpressionCounter(), condition, trueExpr, falseExpr)
    }

    // Statement factories
    fun newEmptyStatement(position: Position): EmptyStatement {
        return EmptyStatement(incStatementCounter(), position)
    }

    fun newGotoStatement(id: Identifier): GotoStatement {
        return GotoStatement(incStatementCounter(), id)
    }

    fun newLabeledStatement(label: Identifier, stmt: Statement): LabeledStatement {
        return LabeledStatement(incStatementCounter(), label, stmt)
    }

    fun newContinueStatement(continueKeyword: Keyword): ContinueStatement {
        return ContinueStatement(incStatementCounter(), continueKeyword)
    }

    fun newBreakStatement(breakKeyword: Keyword): BreakStatement {
        return BreakStatement(incStatementCounter(), breakKeyword)
    }

    fun newDefaultStatement(defaultKeyword: Keyword, stmt: Statement): DefaultStatement {
        return DefaultStatement(incStatementCounter(), defaultKeyword, stmt)
    }

    fun newReturnStatement(retKeyword: Keyword, expr: Expression): ReturnStatement {
        return ReturnStatement(incStatementCounter(), retKeyword, expr)
    }

    fun newCaseStatement(caseKeyword: Keyword, constExpression: Expression, stmt: Statement): CaseStatement {
        return CaseStatement(incStatementCounter(), caseKeyword, constExpression, stmt)
    }

    fun newCompoundStatement(statements: List<CompoundStmtItem>): CompoundStatement {
        return CompoundStatement(incStatementCounter(), statements)
    }

    fun newExprStatement(expr: Expression): ExprStatement {
        return ExprStatement(incStatementCounter(), expr)
    }

    fun newIfStatement(ifKeyword: Keyword, condition: Expression, thenStmt: Statement): IfStatement {
        return IfStatement(incStatementCounter(), ifKeyword, condition, thenStmt)
    }

    fun newIfElseStatement(ifKeyword: Keyword, condition: Expression, thenStmt: Statement, elseStmt: Statement): IfElseStatement {
        return IfElseStatement(incStatementCounter(), ifKeyword, condition, thenStmt, elseStmt)
    }

    fun newWhileStatement(whileKeyword: Keyword, condition: Expression, stmt: Statement): WhileStatement {
        return WhileStatement(incStatementCounter(), whileKeyword, condition, stmt)
    }

    fun newDoWhileStatement(doKeyword: Keyword, stmt: Statement, condition: Expression): DoWhileStatement {
        return DoWhileStatement(incStatementCounter(), doKeyword, stmt,  condition)
    }

    fun newForStatement(forKeyword: Keyword, init: ForInit, condition: Expression, increment: Expression, body: Statement): ForStatement {
        return ForStatement(incStatementCounter(), forKeyword, init, condition, increment, body)
    }

    fun newSwitchStatement(switchKeyword: Keyword, condition: Expression, body: Statement): SwitchStatement {
        return SwitchStatement(incStatementCounter(), switchKeyword, condition, body)
    }

    // TypeSpecifier factories

    fun newDeclarationSpecifier(specifiers: List<AnyTypeNode>): DeclarationSpecifier {
        return DeclarationSpecifier(incTypeSpecCounter(), specifiers)
    }

    fun newTypeName(specifiers: DeclarationSpecifier, abstractDeclarator: AbstractDeclarator?): TypeName {
        return TypeName(incTypeSpecCounter(), specifiers, abstractDeclarator)
    }
}