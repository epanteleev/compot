package parser.nodes

import tokenizer.Position
import tokenizer.tokens.Identifier
import tokenizer.tokens.Keyword

class NodeFabric {
    private var declCounter = 0

    // New declarator factories
    fun newDeclarator(directDeclarator: DirectDeclarator, pointers: List<NodePointer>): Declarator {
        return Declarator(declCounter++, directDeclarator, pointers)
    }

    fun newInitDeclarator(declarator: Declarator, initializer: Initializer): InitDeclarator {
        return InitDeclarator(declCounter++, declarator, initializer)
    }

    //Expression factories
    fun newBinaryOp(left: Expression, right: Expression, op: BinaryOpType): BinaryOp {
        return BinaryOp(left, right, op)
    }

    // Statement factories
    fun newEmptyStatement(position: Position): EmptyStatement {
        return EmptyStatement(position)
    }

    fun newGotoStatement(id: Identifier): GotoStatement {
        return GotoStatement(id)
    }

    fun newLabeledStatement(label: Identifier, stmt: Statement): LabeledStatement {
        return LabeledStatement(label, stmt)
    }

    fun newContinueStatement(continueKeyword: Keyword): ContinueStatement {
        return ContinueStatement(continueKeyword)
    }

    fun newBreakStatement(breakKeyword: Keyword): BreakStatement {
        return BreakStatement(breakKeyword)
    }

    fun newDefaultStatement(defaultKeyword: Keyword, stmt: Statement): DefaultStatement {
        return DefaultStatement(defaultKeyword, stmt)
    }

    fun newReturnStatement(retKeyword: Keyword, expr: Expression): ReturnStatement {
        return ReturnStatement(retKeyword, expr)
    }

    fun newCaseStatement(caseKeyword: Keyword, constExpression: Expression, stmt: Statement): CaseStatement {
        return CaseStatement(caseKeyword, constExpression, stmt)
    }

    fun newCompoundStatement(statements: List<CompoundStmtItem>): CompoundStatement {
        return CompoundStatement(statements)
    }

    fun newExprStatement(expr: Expression): ExprStatement {
        return ExprStatement(expr)
    }

    fun newIfStatement(ifKeyword: Keyword, condition: Expression, thenStmt: Statement): IfStatement {
        return IfStatement(ifKeyword, condition, thenStmt)
    }

    fun newIfElseStatement(ifKeyword: Keyword, condition: Expression, thenStmt: Statement, elseStmt: Statement): IfElseStatement {
        return IfElseStatement(ifKeyword, condition, thenStmt, elseStmt)
    }

    fun newWhileStatement(whileKeyword: Keyword, condition: Expression, stmt: Statement): WhileStatement {
        return WhileStatement(whileKeyword, condition, stmt)
    }

    fun newDoWhileStatement(doKeyword: Keyword, stmt: Statement, condition: Expression): DoWhileStatement {
        return DoWhileStatement(doKeyword, stmt,  condition)
    }

    fun newForStatement(forKeyword: Keyword, init: ForInit, condition: Expression, increment: Expression, body: Statement): ForStatement {
        return ForStatement(forKeyword, init, condition, increment, body)
    }
}