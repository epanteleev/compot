package parser.nodes

import tokenizer.*
import types.*


enum class BinaryOpType {
    ADD {
        override fun toString(): String = "+"
    },
    SUB {
        override fun toString(): String = "-"
    },
    MUL {
        override fun toString(): String = "*"
    },
    DIV {
        override fun toString(): String = "/"
    },
    MOD {
        override fun toString(): String = "%"
    },
    LT {
        override fun toString(): String = "<"
    },
    GT {
        override fun toString(): String = ">"
    },
    LE {
        override fun toString(): String = "<="
    },
    GE {
        override fun toString(): String = ">="
    },
    EQ {
        override fun toString(): String = "=="
    },
    NE {
        override fun toString(): String = "!="
    },
    AND {
        override fun toString(): String = "&&"
    },
    OR {
        override fun toString(): String = "||"
    },
    BIT_OR {
        override fun toString(): String = "|"
    },
    BIT_AND {
        override fun toString(): String = "&"
    },
    BIT_XOR {
        override fun toString(): String = "^"
    },
    ASSIGN {
        override fun toString(): String = "="
    },
    ADD_ASSIGN {
        override fun toString(): String = "+="
    },
    SUB_ASSIGN {
        override fun toString(): String = "-="
    },
    MUL_ASSIGN {
        override fun toString(): String = "*="
    },
    DIV_ASSIGN {
        override fun toString(): String = "/="
    },
    MOD_ASSIGN {
        override fun toString(): String = "%="
    },
    BIT_AND_ASSIGN {
        override fun toString(): String = "&="
    },
    BIT_OR_ASSIGN {
        override fun toString(): String = "|="
    },
    BIT_XOR_ASSIGN {
        override fun toString(): String = "^="
    },
    SHL_ASSIGN {
        override fun toString(): String = "<<="
    },
    SHR_ASSIGN {
        override fun toString(): String = ">>="
    },
    COMMA {
        override fun toString(): String = ","
    },
    SHL {
        override fun toString(): String = "<<"
    },
    SHR {
        override fun toString(): String = ">>"
    },
}


interface UnaryOpType

enum class PrefixUnaryOpType: UnaryOpType {
    NEG {
        override fun toString(): String = "-"
    },
    NOT {
        override fun toString(): String = "!"
    },
    INC {
        override fun toString(): String = "++"
    },
    DEC {
        override fun toString(): String = "--"
    },
    DEREF {
        override fun toString(): String = "*"
    },
    ADDRESS {
        override fun toString(): String = "&"
    },
    PLUS {
        override fun toString(): String = "+"
    }
}

enum class PostfixUnaryOpType: UnaryOpType {
    DEC {
        override fun toString(): String = "--"
    },
    INC {
        override fun toString(): String = "++"
    }
}

abstract class Node {
    abstract fun<T> accept(visitor: NodeVisitor<T>): T
}

abstract class TypeSpecifier : Node()

data class DeclarationSpecifier(val specifiers: List<TypeProperty>) : TypeSpecifier() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    companion object {
        val EMPTY = DeclarationSpecifier(emptyList())
    }
}

data class TypeName(val specifiers: List<Any>, val abstractDecl: Node) : TypeSpecifier() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class FunctionPointerDeclarator(val declarator: List<Node>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class DirectDeclarator(val decl: AnyDeclarator, val declarators: List<Node>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class VarDeclarator(val ident: Ident) : AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class FunctionPointerParamDeclarator(val declarator: Node, val params: Node): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class Declaration(val declspec: DeclarationSpecifier, val declarators: List<AnyDeclarator>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class Cast(val type: TypeName, val cast: Node) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class UnaryOp(val primary: Expression, val type: UnaryOpType) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class ArrayAccess(val primary: Expression, val expr: Expression) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class SizeOf(val expr: Node) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class IdentNode(val str: Ident) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class VarNode(val str: Ident) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class StringNode(val str: StringLiteral) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class NumNode(val toLong: Numeric) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class SwitchStatement(val condition: Node, val body: Statement): Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class NodePointer(val qualifiers: List<PointerQualifier>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class Declspec(val type: CType, val ident: Ident) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class ArrayDeclarator(val constexpr: Node) : AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

object EmptyDeclarator : AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class CompoundLiteral(val typeName: Node, val initializerList: Node) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class InitializerList(val initializers: List<Node>) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

class MemberAccess(val primary: Node, val ident: Ident) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

class ArrowMemberAccess(val primary: Node, val ident: Ident) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

abstract class Expression : Node()


class EmptyExpression : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

class Conditional(val cond: Node, val eTrue: Node, val eFalse: Node) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class FunctionParams(val params: List<AnyParameter>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

abstract class AnyParameter : Node()

data class Parameter(val declspec: DeclarationSpecifier, val declarator: AnyDeclarator) : AnyParameter() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        val varNode = declarator as Declarator
        return (varNode.declspec.decl as VarDeclarator).ident.str()
    }
}

class ParameterVarArg: AnyParameter() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class FunctionNode(val specifier: DeclarationSpecifier, val declarator: Declarator, val body: Statement) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        val varNode = declarator.declspec.decl as VarDeclarator
        return varNode.ident.str()
    }
}

class EmptyStatement : Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class LabeledStatement(val label: Ident, val stmt: Statement) : Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class GotoStatement(val id: Ident) : Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

class ContinueStatement : Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

class BreakStatement : Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

class DefaultStatement(val stmt: Statement) : Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

class CaseStatement(val expr: Node, val stmt: Statement) : Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

abstract class Statement: Node()

data class ReturnStatement(val expr: Expression): Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class CompoundStatement(val statements: List<Node>): Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class ExprStatement(val expr: Expression): Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

abstract class AnyDeclarator: Node()

data class Declarator(val declspec: DirectDeclarator, val pointers: List<NodePointer>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class AssignmentDeclarator(val rvalue: Declarator, val lvalue: Expression): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class FunctionDeclarator(val params: List<AnyParameter>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class IfStatement(val condition: Expression, val then: Statement, val elseNode: Statement): Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class BinaryOp(val left: Expression, val right: Expression, val type: BinaryOpType) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class DoWhileStatement(val body: Statement, val condition: Expression): Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class WhileStatement(val condition: Expression, val body: Statement): Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class ForStatement(val init: Node, val condition: Node, val update: Node, val body: Node): Statement() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class ProgramNode(val nodes: MutableList<Node>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class AbstractDeclarator(val pointers: List<NodePointer>, val directAbstractDeclarator: List<Node>) : AnyDeclarator() {   //TODO
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class DirectFunctionDeclarator(val parameters: List<AnyParameter>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class DirectArrayDeclarator(val size: Node) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class FunctionCall(val primary: Node, val args: List<Node>) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class StructField(val declspec: List<Any>, val declarators: List<Node>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

class Enumerator(val ident: Ident, val expr: Node) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

abstract class AnyTypeNode : Node(), TypeProperty {
    abstract fun name(): String
}

data class TypeNode(val ident: Ident) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()
}

data class UnionSpecifier(val ident: Ident, val fields: List<StructField>) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()
}

data class UnionDeclaration(val name: Ident) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()
}

data class StructDeclaration(val name: Ident) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()
}

data class StructSpecifier(val ident: Ident, val fields: List<StructField>) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()
}

data class EnumSpecifier(val ident: Ident, val enumerators: List<Node>) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()
}

data class EnumDeclaration(val name: Ident) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()
}

object DummyNode : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}