package parser.nodes

import parser.LabelResolver
import tokenizer.tokens.Identifier
import parser.nodes.visitors.StatementVisitor
import tokenizer.Position
import tokenizer.tokens.Keyword


sealed class Statement(private val id: Int) {
    abstract fun begin(): Position
    abstract fun<T> accept(visitor: StatementVisitor<T>): T

    final override fun hashCode(): Int = id
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Statement

        return id == other.id
    }
}

class EmptyStatement(id: Int, private val position: Position) : Statement(id) {
    override fun begin(): Position = position
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class LabeledStatement(id: Int, val label: Identifier, val stmt: Statement) : Statement(id) {
    override fun begin(): Position = label.position()
    private var gotos = arrayListOf<GotoStatement>()

    fun name(): String = label.str()

    fun gotos(): MutableList<GotoStatement> = gotos

    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class GotoStatement(i: Int, val id: Identifier) : Statement(i) {
    override fun begin(): Position = id.position()
    private var label: LabeledStatement? = null

    fun label(): LabeledStatement? = label

    internal fun resolve(resolver: LabelResolver): LabeledStatement? {
        label = resolver.resolve(id)
        return label
    }

    fun name(): String = id.str()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ContinueStatement(id: Int, private val contKeyword: Keyword) : Statement(id) {
    override fun begin(): Position = contKeyword.position()
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class BreakStatement(id: Int, private val breakKeyword: Keyword) : Statement(id) {
    override fun begin(): Position = breakKeyword.position()
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class DefaultStatement(id: Int, private val defaultKeyword: Keyword, val stmt: Statement) : Statement(id) {
    override fun begin(): Position = defaultKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class CaseStatement(id: Int, private val caseKeyword: Keyword, val constExpression: Expression, val stmt: Statement) : Statement(id) {
    override fun begin(): Position = caseKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ReturnStatement(id: Int, private val retKeyword: Keyword, val expr: Expression): Statement(id) {
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

class CompoundStatement(id: Int, val statements: List<CompoundStmtItem>): Statement(id) {
    override fun begin(): Position = statements.first().begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class ExprStatement(id: Int, val expr: Expression): Statement(id) {
    override fun begin(): Position = expr.begin()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class IfElseStatement(id: Int, private val ifKeyword: Keyword, val condition: Expression, val then: Statement, val elseNode: Statement): Statement(id) {
    override fun begin(): Position = ifKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class IfStatement(id: Int, private val ifKeyword: Keyword, val condition: Expression, val then: Statement): Statement(id) {
    override fun begin(): Position = ifKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class DoWhileStatement(id: Int, private val doKeyword: Keyword, val body: Statement, val condition: Expression): Statement(id) {
    override fun begin(): Position = doKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class WhileStatement(id: Int, private val whileKeyword: Keyword, val condition: Expression, val body: Statement): Statement(id) {
    override fun begin(): Position = whileKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

sealed class ForInit
data class ForInitDeclaration(val declaration: Declaration): ForInit()
data class ForInitExpression(val expression: ExprStatement): ForInit()
data object ForInitEmpty: ForInit()

class ForStatement(id: Int, private val forKeyword: Keyword, val init: ForInit, val condition: Expression, val update: Expression, val body: Statement): Statement(id) {
    override fun begin(): Position = forKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}

class SwitchStatement(id: Int, private val switchKeyword: Keyword, val condition: Expression, val body: Statement): Statement(id) {
    override fun begin(): Position = switchKeyword.position()
    override fun<T> accept(visitor: StatementVisitor<T>) = visitor.visit(this)
}