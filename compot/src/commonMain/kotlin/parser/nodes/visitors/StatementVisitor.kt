package parser.nodes.visitors

import parser.nodes.*


interface StatementVisitor<T> {
    fun visit(emptyStatement: EmptyStatement): T
    fun visit(exprStatement: ExprStatement): T
    fun visit(labeledStatement: LabeledStatement): T
    fun visit(gotoStatement: GotoStatement): T
    fun visit(continueStatement: ContinueStatement): T
    fun visit(breakStatement: BreakStatement): T
    fun visit(defaultStatement: DefaultStatement): T
    fun visit(caseStatement: CaseStatement): T
    fun visit(returnStatement: ReturnStatement): T
    fun visit(compoundStatement: CompoundStatement): T
    fun visit(ifElseStatement: IfElseStatement): T
    fun visit(ifStatement: IfStatement): T
    fun visit(doWhileStatement: DoWhileStatement): T
    fun visit(whileStatement: WhileStatement): T
    fun visit(forStatement: ForStatement): T
    fun visit(switchStatement: SwitchStatement): T
}