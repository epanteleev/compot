package parser.nodes

import types.*
import typedesc.*
import tokenizer.tokens.*
import common.assertion
import parser.nodes.visitors.*
import sema.SemanticAnalysis
import tokenizer.Position


sealed class Expression(private val id: Int) {
    abstract fun begin(): Position
    abstract fun<T> accept(visitor: ExpressionVisitor<T>): T

    final override fun hashCode(): Int = id
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Expression

        return id == other.id
    }
}

// https://port70.net/~nsz/c/c11/n1570.html#6.5.2.5
// 6.5.2.5 Compound literals
class CompoundLiteral internal constructor(id: Int, val typeName: TypeName, val initializerList: InitializerList) : Expression(id) {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
    override fun begin(): Position = typeName.begin()
}

class BinaryOp internal constructor(id: Int, val left: Expression, val right: Expression, val opType: BinaryOpType) : Expression(id) {
    override fun begin(): Position = left.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class EmptyExpression internal constructor(id: Int, private val where: Position) : Expression(id) {
    override fun begin(): Position = where
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class Conditional internal constructor(id: Int, val cond: Expression, val eTrue: Expression, val eFalse: Expression) : Expression(id) {
    override fun begin(): Position = cond.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class FunctionCall internal constructor(id: Int, val primary: Expression, val args: List<Expression>) : Expression(id) {
    override fun begin(): Position = primary.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class MemberAccess internal constructor(id: Int, val primary: Expression, val fieldName: Identifier) : Expression(id) {
    override fun begin(): Position = primary.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    fun memberName(): String = fieldName.str()
}

class ArrowMemberAccess internal constructor(id: Int, val primary: Expression, private val ident: Identifier) : Expression(id) {
    override fun begin(): Position = primary.begin()
    fun fieldName(): String = ident.str()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class VarNode internal constructor(id: Int, private val str: Identifier) : Expression(id) {
    override fun begin(): Position = str.position()
    fun name(): String = str.str()
    fun nameIdent(): Identifier = str

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class StringNode internal constructor(id: Int, val literals: List<StringLiteral>) : Expression(id) {
    init {
        assertion(literals.isNotEmpty()) { "Empty string node" }
    }

    private val data by lazy {
        if (literals.all { it.isEmpty() }) {
            ""
        } else {
            literals.joinToString("", postfix = "") { it.data() }
        }
    }

    override fun begin(): Position = literals.first().position()

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    fun length(): Int = data.length + 1
    fun isNotEmpty(): Boolean = data.isNotEmpty()
    fun data(): String = data
}

class CharNode internal constructor(id: Int, val char: CharLiteral) : Expression(id) {
    override fun begin(): Position = char.position()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    fun toByte(): Byte = char.code()
}

class NumNode internal constructor(id: Int, val number: PPNumber) : Expression(id) {
    override fun begin(): Position = number.position()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class UnaryOp internal constructor(id: Int, val primary: Expression, val opType: UnaryOpType) : Expression(id) {
    override fun begin(): Position = primary.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class ArrayAccess internal constructor(id: Int, val primary: Expression, val expr: Expression) : Expression(id) {
    override fun begin(): Position = primary.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

sealed class SizeOfParam {
    abstract fun begin(): Position
    abstract fun constEval(typeHolder: TypeHolder): Int
}

class SizeOfType(val typeName: TypeName) : SizeOfParam() {
    override fun begin(): Position = typeName.begin()
    override fun constEval(typeHolder: TypeHolder): Int {
        val resolved = typeName.specifyType(typeHolder).typeDesc.cType()
        if (resolved !is CompletedType) {
            throw TypeResolutionException("sizeof on uncompleted type: $resolved", begin())
        }

        return resolved.size()
    }
}

class SizeOfExpr(val expr: Expression) : SizeOfParam() {
    override fun begin(): Position = expr.begin()
    override fun constEval(typeHolder: TypeHolder): Int = expr.accept(SemanticAnalysis(typeHolder)).size()
}

class SizeOf internal constructor(id: Int, val expr: SizeOfParam) : Expression(id) {
    override fun begin(): Position = expr.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    fun constEval(typeHolder: TypeHolder): Int = expr.constEval(typeHolder)
}

class Cast internal constructor(id: Int, val typeName: TypeName, val cast: Expression) : Expression(id) {
    override fun begin(): Position = typeName.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class BuiltinVaArg internal constructor(id: Int, val assign: Expression, val typeName: TypeName) : Expression(id) {
    override fun begin(): Position = assign.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class BuiltinVaStart internal constructor(id: Int, val vaList: Expression, val param: Expression) : Expression(id) {
    override fun begin(): Position = vaList.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class BuiltinVaEnd internal constructor(id: Int, val vaList: Expression) : Expression(id) {
    override fun begin(): Position = vaList.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}

class BuiltinVaCopy internal constructor(id: Int, val dest: Expression, val src: Expression) : Expression(id) {
    override fun begin(): Position = dest.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
}