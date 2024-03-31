package parser.nodes

import types.Type
import tokenizer.*


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
    abstract fun accept(visitor: NodeVisitor)
}

abstract class TypeSpecifier : Node()

data class DeclarationSpecifier(val specifiers: List<Any>) : TypeSpecifier() {
    override fun accept(visitor: NodeVisitor) = visitor.visit(this)
}

data class TypeName(val specifiers: List<Any>, val abstractDecl: Node) : TypeSpecifier() {
    override fun accept(visitor: NodeVisitor) = visitor.visit(this)
}

data class FunctionPointerDeclarator(val declarator: List<Node>): AnyDeclarator() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class FunctionPointerParamDeclarator(val declarator: Node, val params: Node): AnyDeclarator() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class Declaration(val declspec: Node, val declarators: List<Node>): Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class Cast(val type: TypeName, val cast: Node) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class UnaryOp(val primary: Node, val type: UnaryOpType) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class ArrayAccess(val primary: Node, val expr: Node) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class SizeOf(val expr: Node) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class IdentNode(val str: Ident) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class VarNode(val str: Ident) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class StringNode(val str: StringLiteral) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class NumNode(val toLong: Numeric) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class SwitchStatement(val condition: Node, val body: Statement): Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

enum class PointerQualifier {
    CONST {
        override fun toString() = "const"
    },
    VOLATILE {
        override fun toString() = "volatile"
    },
    RESTRICT {
        override fun toString() = "restrict"
    }
}

data class Pointer(val qualifiers: List<PointerQualifier>) : Node() {
    override fun accept(visitor: NodeVisitor) = visitor.visit(this)
}

data class Pointers(val pointers: List<Pointer>) : Node() {
    override fun accept(visitor: NodeVisitor) = visitor.visit(this)
}

data class Declspec(val type: Type, val ident: Ident) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class ArrayDeclarator(val constexpr: Node) : AnyDeclarator() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class CompoundLiteral(val typeName: Node, val initializerList: Node) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class InitializerList(val initializers: List<Node>) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class MemberAccess(val primary: Node, val ident: Ident) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class ArrowMemberAccess(val primary: Node, val ident: Ident) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

abstract class Expression : Node()

class Conditional(val cond: Node, val eTrue: Node, val eFalse: Node) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class FunctionParams(val params: List<AnyParameter>): Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

abstract class AnyParameter : Node()

data class Parameter(val declspec: Node, val declarator: Node) : AnyParameter() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class ParameterVarArg: AnyParameter() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class FunctionNode(val type: Node, val declarator: Node, val body: Statement) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }

}

class EmptyStatement : Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class LabeledStatement(val label: Ident, val stmt: Statement) : Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class GotoStatement(val id: Ident) : Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class ContinueStatement : Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class BreakStatement : Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class DefaultStatement(val stmt: Statement) : Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class CaseStatement(val expr: Node, val stmt: Statement) : Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

abstract class Statement: Node()

data class ReturnStatement(val expr: Node): Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class CompoundStatement(val statements: List<Node>): Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class ExprStatement(val expr: Node): Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

abstract class AnyDeclarator: Node()

data class Declarator(val declspec: List<Node>, val pointers: Node): AnyDeclarator() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class AssignmentDeclarator(val rvalue: Node, val lvalue: Node): AnyDeclarator() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class RValueDeclarator(val rvalue: Node): AnyDeclarator() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class FunctionDeclarator(val params: List<AnyParameter>): AnyDeclarator() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class IfStatement(val condition: Node, val then: Node, val elseNode: Node): Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class BinaryOp(val cond: Node, val assign: Node, val type: BinaryOpType) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class DoWhileStatement(val body: Node, val condition: Node): Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class WhileStatement(val condition: Node, val body: Node): Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class ForStatement(val init: Node, val condition: Node, val update: Node, val body: Node): Statement() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class ProgramNode(val nodes: MutableList<Node>) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

enum class StorageClass {
    TYPEDEF {
        override fun toString(): String = "typedef"
    },
    EXTERN {
        override fun toString(): String = "extern"
    },
    STATIC {
        override fun toString(): String = "static"
    },
    REGISTER {
        override fun toString(): String = "register"
    },
    AUTO {
        override fun toString(): String = "auto"
    }
}

data class AbstractDeclarator(val pointer: Node, val directAbstractDeclarator: List<Node>) : Node() {   //TODO
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class DirectFunctionDeclarator(val parameters: List<AnyParameter>) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class DirectArrayDeclarator(val size: Node) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class FunctionCall(val primary: Node, val args: List<Node>) : Expression() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class StructField(val declspec: List<Any>, val declarators: List<Node>): Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class Enumerator(val ident: Ident, val expr: Node) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

abstract class AnyTypeNode : Node()

data class TypeNode(val node: Node) : AnyTypeNode() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class UnionSpecifier(val ident: Ident, val fields: List<StructField>) : AnyTypeNode() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class UnionDeclaration(val name: Ident) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class StructDeclaration(val name: Ident) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class StructSpecifier(val ident: Ident, val fields: List<StructField>) : AnyTypeNode() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class EnumSpecifier(val ident: Ident, val enumerators: List<Node>) : AnyTypeNode() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

data class EnumDeclaration(val name: Ident) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

object DummyNode : Node() {
    override fun accept(visitor: NodeVisitor) = visitor.visit(this)
}