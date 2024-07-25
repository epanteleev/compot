package parser.nodes

import parser.nodes.visitors.StatementVisitor
import tokenizer.Identifier


abstract class Statement: Node() {
    abstract fun<T> accept(visitor: StatementVisitor<T>): T
}

object EmptyStatement : Statement() {
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
}