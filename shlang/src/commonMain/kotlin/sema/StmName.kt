package sema

import parser.nodes.*
import parser.nodes.visitors.StatementVisitor

internal object StmName: StatementVisitor<String> {
    override fun visit(emptyStatement: EmptyStatement): String = "EmptyStatement"
    override fun visit(exprStatement: ExprStatement): String = "ExprStatement"
    override fun visit(labeledStatement: LabeledStatement): String = "LabeledStatement(${labeledStatement.name()})"
    override fun visit(gotoStatement: GotoStatement): String = "GotoStatement(${gotoStatement.name()})"
    override fun visit(continueStatement: ContinueStatement): String = "ContinueStatement"
    override fun visit(breakStatement: BreakStatement): String = "BreakStatement"
    override fun visit(defaultStatement: DefaultStatement): String = "DefaultStatement"
    override fun visit(caseStatement: CaseStatement): String = "CaseStatement"
    override fun visit(returnStatement: ReturnStatement): String = "ReturnStatement"
    override fun visit(compoundStatement: CompoundStatement): String = "CompoundStatement"
    override fun visit(ifElseStatement: IfElseStatement): String = "IfElseStatement"
    override fun visit(ifStatement: IfStatement): String = "IfStatement"
    override fun visit(doWhileStatement: DoWhileStatement): String = "DoWhileStatement"
    override fun visit(whileStatement: WhileStatement): String = "WhileStatement"
    override fun visit(forStatement: ForStatement): String = "ForStatement"
    override fun visit(switchStatement: SwitchStatement): String = "SwitchStatement"
}