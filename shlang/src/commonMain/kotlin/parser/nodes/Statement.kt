package parser.nodes

import parser.nodes.visitors.StatementVisitor
import tokenizer.Identifier


abstract class Statement: Node() {
    abstract fun<T> accept(visitor: StatementVisitor<T>): T
}

class EmptyStatement : Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class LabeledStatement(val label: Identifier, val stmt: Statement) : Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class GotoStatement(val id: Identifier) : Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ContinueStatement : Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class BreakStatement : Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class DefaultStatement(val stmt: Statement) : Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class CaseStatement(val constExpression: Expression, val stmt: Statement) : Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class ReturnStatement(val expr: Expression): Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class CompoundStatement(val statements: List<Node>): Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class ExprStatement(val expr: Expression): Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class IfStatement(val condition: Expression, val then: Statement, val elseNode: Statement): Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class DoWhileStatement(val body: Statement, val condition: Expression): Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class WhileStatement(val condition: Expression, val body: Statement): Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class ForStatement(val init: Node, val condition: Expression, val update: Expression, val body: Statement): Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class SwitchStatement(val condition: Expression, val body: Statement): Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)

    fun default(visit: (DefaultStatement) -> Unit) {
        body.accept(object: StatementVisitor<Unit> {
            override fun visit(emptyStatement: EmptyStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(exprStatement: ExprStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(labeledStatement: LabeledStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(gotoStatement: GotoStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(continueStatement: ContinueStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(breakStatement: BreakStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(defaultStatement: DefaultStatement) {
                visit(defaultStatement)
            }

            override fun visit(node: CaseStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(returnStatement: ReturnStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(compoundStatement: CompoundStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(ifStatement: IfStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(doWhileStatement: DoWhileStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(whileStatement: WhileStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(forStatement: ForStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(switchStatement: SwitchStatement) {
                TODO("Not yet implemented")
            }
        })
    }

    fun cases(visit: (CaseStatement) -> Unit) {
        body.accept(object: StatementVisitor<Unit> {
            override fun visit(emptyStatement: EmptyStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(exprStatement: ExprStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(labeledStatement: LabeledStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(gotoStatement: GotoStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(continueStatement: ContinueStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(breakStatement: BreakStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(defaultStatement: DefaultStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(node: CaseStatement) {
                visit(node)
            }

            override fun visit(returnStatement: ReturnStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(compoundStatement: CompoundStatement) {
                compoundStatement.statements.forEach {
                    if (it is Statement) {
                        it.accept(this)
                    }
                }
            }

            override fun visit(ifStatement: IfStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(doWhileStatement: DoWhileStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(whileStatement: WhileStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(forStatement: ForStatement) {
                TODO("Not yet implemented")
            }

            override fun visit(switchStatement: SwitchStatement) {
                TODO("Not yet implemented")
            }
        })
    }
}