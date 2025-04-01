package parser.nodes

import parser.LabelResolver
import tokenizer.tokens.Identifier
import parser.nodes.visitors.StatementVisitor
import tokenizer.Position
import tokenizer.tokens.Keyword


sealed class Statement {
    abstract fun begin(): Position
    abstract fun<T> accept(visitor: StatementVisitor<T>): T
}

class EmptyStatement(private val position: Position) : Statement() {
    override fun begin(): Position = position
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class LabeledStatement(val label: Identifier, val stmt: Statement) : Statement() {
    override fun begin(): Position = label.position()
    private var gotos = hashSetOf<GotoStatement>()

    fun name(): String = label.str()

    fun gotos(): MutableSet<GotoStatement> = gotos

    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class GotoStatement(val id: Identifier) : Statement() {
    override fun begin(): Position = id.position()
    private var label: LabeledStatement? = null

    fun label(): LabeledStatement? = label

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GotoStatement

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    internal fun resolve(resolver: LabelResolver): LabeledStatement? {
        label = resolver.resolve(id)
        return label
    }

    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ContinueStatement(private val contKeyword: Keyword) : Statement() {
    override fun begin(): Position = contKeyword.position()
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class BreakStatement(private val breakKeyword: Keyword) : Statement() {
    override fun begin(): Position = breakKeyword.position()
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class DefaultStatement(private val defaultKeyword: Keyword, val stmt: Statement) : Statement() {
    override fun begin(): Position = defaultKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class CaseStatement(private val caseKeyword: Keyword, val constExpression: Expression, val stmt: Statement) : Statement() {
    override fun begin(): Position = caseKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ReturnStatement(private val retKeyword: Keyword, val expr: Expression): Statement() {
    override fun begin(): Position = retKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

sealed class CompoundStmtItem {
    abstract fun begin(): Position
}

data class CompoundStmtDeclaration(val declaration: Declaration): CompoundStmtItem() {
    override fun begin(): Position = declaration.begin()
}
data class CompoundStmtStatement(val statement: Statement): CompoundStmtItem() {
    override fun begin(): Position = statement.begin()
}

class CompoundStatement(val statements: List<CompoundStmtItem>): Statement() {
    override fun begin(): Position = statements.first().begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ExprStatement(val expr: Expression): Statement() {
    override fun begin(): Position = expr.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class IfElseStatement(private val ifKeyword: Keyword, val condition: Expression, val then: Statement, val elseNode: Statement): Statement() {
    override fun begin(): Position = ifKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class IfStatement(private val ifKeyword: Keyword, val condition: Expression, val then: Statement): Statement() {
    override fun begin(): Position = ifKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class DoWhileStatement(private val doKeyword: Keyword, val body: Statement, val condition: Expression): Statement() {
    override fun begin(): Position = doKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class WhileStatement(private val whileKeyword: Keyword, val condition: Expression, val body: Statement): Statement() {
    override fun begin(): Position = whileKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

sealed class ForInit
data class ForInitDeclaration(val declaration: Declaration): ForInit()
data class ForInitExpression(val expression: ExprStatement): ForInit()
object ForInitEmpty: ForInit()

class ForStatement(private val forKeyword: Keyword, val init: ForInit, val condition: Expression, val update: Expression, val body: Statement): Statement() {
    override fun begin(): Position = forKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class SwitchStatement(private val switchKeyword: Keyword, val condition: Expression, val body: Statement): Statement() {
    override fun begin(): Position = switchKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}