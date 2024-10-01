package parser.nodes

import parser.LabelResolver
import tokenizer.tokens.Identifier
import parser.nodes.visitors.StatementVisitor


sealed class Statement: Node() {
    abstract fun<T> accept(visitor: StatementVisitor<T>): T
}

data object EmptyStatement : Statement() {
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class LabeledStatement(val label: Identifier, val stmt: Statement) : Statement() {
    private var gotos = hashSetOf<GotoStatement>()

    fun name(): String = label.str()

    fun gotos(): MutableSet<GotoStatement> = gotos

    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class GotoStatement(val id: Identifier) : Statement() {
    private var label: LabeledStatement? = null

    fun label(): LabeledStatement? = label

    internal fun resolve(resolver: LabelResolver): LabeledStatement? {
        label = resolver.resolve(id)
        return label
    }

    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data object ContinueStatement : Statement() {
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data object BreakStatement : Statement() {
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
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