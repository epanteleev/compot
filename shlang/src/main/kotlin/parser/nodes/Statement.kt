package parser.nodes

import parser.nodes.visitors.StatementVisitor
import tokenizer.Ident


abstract class Statement: Node() {
    abstract fun<T> accept(visitor: StatementVisitor<T>): T
}


class EmptyStatement : Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class LabeledStatement(val label: Ident, val stmt: Statement) : Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class GotoStatement(val id: Ident) : Statement() {
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

class CaseStatement(val expr: Node, val stmt: Statement) : Statement() {
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

data class SwitchStatement(val condition: Node, val body: Statement): Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}