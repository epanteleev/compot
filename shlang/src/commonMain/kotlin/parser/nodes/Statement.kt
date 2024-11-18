package parser.nodes

import parser.LabelResolver
import tokenizer.tokens.Identifier
import parser.nodes.visitors.StatementVisitor
import tokenizer.Position
import tokenizer.tokens.Keyword


sealed class Statement: Node() {
    abstract fun<T> accept(visitor: StatementVisitor<T>): T
}

object EmptyStatement : Statement() {
    override fun begin(): Position {
        TODO("Not yet implemented")
    }
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class LabeledStatement(val label: Identifier, val stmt: Statement) : Statement() {
    override fun begin(): Position = label.position()
    private var gotos = hashSetOf<GotoStatement>()

    fun name(): String = label.str()

    fun gotos(): MutableSet<GotoStatement> = gotos

    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

data class GotoStatement(val id: Identifier) : Statement() {
    override fun begin(): Position = id.position()
    private var label: LabeledStatement? = null

    fun label(): LabeledStatement? = label

    internal fun resolve(resolver: LabelResolver): LabeledStatement? {
        label = resolver.resolve(id)
        return label
    }

    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ContinueStatement(val contKeyword: Keyword) : Statement() {
    override fun begin(): Position = contKeyword.position()
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class BreakStatement(val breakKeyword: Keyword) : Statement() {
    override fun begin(): Position = breakKeyword.position()
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class DefaultStatement(val stmt: Statement) : Statement() {
    override fun begin(): Position = stmt.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class CaseStatement(val constExpression: Expression, val stmt: Statement) : Statement() {
    override fun begin(): Position = constExpression.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ReturnStatement(val expr: Expression): Statement() {
    override fun begin(): Position = expr.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class CompoundStatement(val statements: List<Node>): Statement() {
    override fun begin(): Position = statements.first().begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ExprStatement(val expr: Expression): Statement() {
    override fun begin(): Position = expr.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class IfStatement(val condition: Expression, val then: Statement, val elseNode: Statement): Statement() {
    override fun begin(): Position = condition.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class DoWhileStatement(val body: Statement, val condition: Expression): Statement() {
    override fun begin(): Position = body.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class WhileStatement(val condition: Expression, val body: Statement): Statement() {
    override fun begin(): Position = condition.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ForStatement(val init: Node, val condition: Expression, val update: Expression, val body: Statement): Statement() {
    override fun begin(): Position = init.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class SwitchStatement(val condition: Expression, val body: Statement): Statement() {
    override fun begin(): Position = condition.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}