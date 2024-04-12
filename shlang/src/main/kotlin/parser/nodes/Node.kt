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

data class DeclarationSpecifier(val specifiers: List<AnyTypeNode>) : TypeSpecifier(), Resolvable {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        val ctypeBuilder = CTypeBuilder()
        specifiers.forEach {
            when (it) {
                is TypeNode -> {
                    ctypeBuilder.add(it.type()) //TODO
                }
                is TypeQualifier -> {
                    ctypeBuilder.add(it.qualifier())
                }
                is StorageClassSpecifier -> {
                    ctypeBuilder.add(it.storageClass())
                }
                is StructSpecifier -> {
                    TODO()
                }
                is UnionSpecifier -> {
                    TODO()
                }
                is EnumSpecifier -> {
                    TODO()
                }
                is EnumDeclaration -> {
                    TODO()
                }
                is StructDeclaration -> {
                    ctypeBuilder.add(it.typeResolver(typeHolder))
                }
            }
        }

        return ctypeBuilder.build(typeHolder)
    }

    companion object {
        val EMPTY = DeclarationSpecifier(emptyList())
    }
}

data class TypeName(val specifiers: List<Any>, val abstractDecl: Node) : TypeSpecifier(), Resolvable {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }
}

data class FunctionPointerDeclarator(val declarator: List<Node>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class DirectDeclarator(val decl: AnyDeclarator, val declarators: List<Node>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return when (decl) {
            is VarDeclarator -> decl.name()
            is Declarator -> decl.name()
            else -> throw IllegalStateException("$decl")
        }
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class VarDeclarator(val ident: Ident) : AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String = ident.str()
    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class FunctionPointerParamDeclarator(val declarator: Node, val params: Node): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class Declaration(val declspec: DeclarationSpecifier, val declarators: List<AnyDeclarator>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun resolveType(typeHolder: TypeHolder) {
        val type = declspec.resolveType(typeHolder)
        declarators.forEach {
            when (it) {
                is Declarator           -> it.resolveType(type, typeHolder)
                is AssignmentDeclarator -> it.resolveType(type, typeHolder)
                else -> TODO()
            }
        }
    }
}

data class Cast(val typeName: TypeName, val cast: Node) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        type = typeName.resolveType(typeHolder)
        return type
    }
}

data class UnaryOp(val primary: Expression, val opType: UnaryOpType) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val primaryType = primary.resolveType(typeHolder)
        if (opType is PrefixUnaryOpType) {
            type = when (opType) {
//                PrefixUnaryOpType.DEREF -> {
//                    if (primaryType is CPointerType) {
//                        primaryType.type
//                    } else {
//                        CType.UNRESOlVED
//                    }
//                }
//                PrefixUnaryOpType.ADDRESS -> {
//                    CPointerType(primaryType)
//                }
                PrefixUnaryOpType.NOT -> {
                    if (primaryType == CType.BOOL) {
                        CType.BOOL
                    } else {
                        CType.UNRESOlVED
                    }
                }
                PrefixUnaryOpType.NEG -> {
                    if (primaryType == CType.INT || primaryType == CType.LONG) {
                        primaryType
                    } else {
                        CType.UNRESOlVED
                    }
                }
                PrefixUnaryOpType.INC, PrefixUnaryOpType.DEC, PrefixUnaryOpType.PLUS -> {
                    primaryType
                }
                else -> TODO()
            }
        } else {
            type = primaryType
        }
        return type

    }
}

data class ArrayAccess(val primary: Expression, val expr: Expression) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val primaryType = primary.resolveType(typeHolder)
        if (primaryType is CArrayType) {
            type = primaryType.type
            return type
        }
        type = CType.UNKNOWN
        return type

    }
}

data class SizeOf(val expr: Node) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        expr as Resolvable
        type = expr.resolveType(typeHolder)
        return type
    }
}

data class IdentNode(val str: Ident) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class VarNode(val str: Ident) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        type = typeHolder[str.str()]
        return type
    }
}

data class StringNode(val str: StringLiteral) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }
        type = CPointerType(CType.CHAR)
        return type
    }
}

data class NumNode(val toLong: Numeric) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }
        when (toLong.data) {
            is Int -> type = CType.INT
            is Long -> type = CType.LONG
            is Float -> type = CType.FLOAT
            is Double -> type = CType.DOUBLE
            else -> type = CType.UNKNOWN
        }
        return type
    }
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

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

object EmptyDeclarator : AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = CType.UNKNOWN
}

data class CompoundLiteral(val typeName: Node, val initializerList: Node) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class InitializerList(val initializers: List<Expression>) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val types = initializers.map { it.resolveType(typeHolder) }
        val commonType = types.reduce { acc, type -> CType.interfereTypes(acc, type) }
        type = commonType
        return commonType
    }
}

class MemberAccess(val primary: Expression, val ident: Ident) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val structType = primary.resolveType(typeHolder)
        if (structType !is CStructType) {
            type = CType.UNKNOWN
            return type
        }
        val field = structType.fields.find { it.first == ident.str() }
        if (field != null) {
            type = field.second
            return type
        }
        type = CType.UNKNOWN
        return CType.UNKNOWN
    }
}

class ArrowMemberAccess(val primary: Expression, val ident: Ident) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType { //TODO Copy-paste?!?
        if (type != CType.UNRESOlVED) {
            return type
        }

        val structType = primary.resolveType(typeHolder)
        if (structType !is CStructType) {
            type = CType.UNKNOWN
            return type
        }
        val field = structType.fields.find { it.first == ident.str() }
        if (field != null) {
            type = field.second
            return type
        }
        type = CType.UNKNOWN
        return type
    }
}

abstract class Expression : Node(), Resolvable {
    protected var type: CType = CType.UNRESOlVED
}

data class BinaryOp(val left: Expression, val right: Expression, val opType: BinaryOpType) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val leftType = left.resolveType(typeHolder)
        val rightType = right.resolveType(typeHolder)
        val commonType = CType.interfereTypes(leftType, rightType)
        type = commonType
        return commonType
    }
}

class EmptyExpression : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = type
}

class Conditional(val cond: Expression, val eTrue: Expression, val eFalse: Expression) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val typeTrue = eTrue.resolveType(typeHolder)
        val typeFalse = eFalse.resolveType(typeHolder)
        val commonType = CType.interfereTypes(typeTrue, typeFalse)
        type = commonType
        return commonType
    }
}

data class FunctionParams(val params: List<AnyParameter>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

abstract class AnyParameter : Node()

data class Parameter(val declspec: DeclarationSpecifier, val declarator: AnyDeclarator) : AnyParameter() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        val varNode = declarator as Declarator
        return (varNode.directDeclarator.decl as VarDeclarator).ident.str()
    }
}

class ParameterVarArg: AnyParameter() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class FunctionNode(val specifier: DeclarationSpecifier, val declarator: Declarator, val body: Statement) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        val varNode = declarator.directDeclarator.decl as VarDeclarator
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

abstract class AnyDeclarator: Node(), Resolvable

data class Declarator(val directDeclarator: DirectDeclarator, val pointers: List<NodePointer>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return directDeclarator.name()
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }

    fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        var pointerType = baseType
        for (pointer in pointers) {
            pointerType = CPointerType(pointerType)
        }

        typeHolder.add(name(), pointerType)
        return pointerType
    }
}

data class AssignmentDeclarator(val rvalue: Declarator, val lvalue: Expression): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }

    fun resolveType(ctype: CType, typeHolder: TypeHolder): CType {
        return rvalue.resolveType(ctype, typeHolder)
    }
}

data class FunctionDeclarator(val params: List<AnyParameter>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class IfStatement(val condition: Expression, val then: Statement, val elseNode: Statement): Statement() {
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

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class DirectFunctionDeclarator(val parameters: List<AnyParameter>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class DirectArrayDeclarator(val size: Node) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class FunctionCall(val primary: Expression, val args: List<Expression>) : Expression() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val funcType = primary.resolveType(typeHolder)
        if (funcType !is FunctionType) {
            type = CType.UNKNOWN
            return type
        }

        val params = funcType.argsTypes
        if (params.size != args.size) {
            type = CType.UNRESOlVED
            return type
        }

        for (i in args.indices) {
            val argType = args[i].resolveType(typeHolder)
            if (argType != params[i]) {
                type = CType.UNRESOlVED
                return type
            }
        }
        type = funcType.retType
        return type
    }
}

data class StructField(val declspec: List<Any>, val declarators: List<Node>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

class Enumerator(val ident: Ident, val expr: Node) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

abstract class AnyTypeNode : Node() {
    abstract fun name(): String
}

data class TypeNode(val ident: Ident) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()

    fun type(): BaseType {
        return when (ident.str()) {
            "void"    -> BaseType.VOID
            "char"    -> BaseType.CHAR
            "short"   -> BaseType.SHORT
            "int"     -> BaseType.INT
            "long"    -> BaseType.LONG
            "float"   -> BaseType.FLOAT
            "double"  -> BaseType.DOUBLE
            "signed"  -> BaseType.INT
            "unsigned"-> BaseType.UINT
            "bool"    -> BaseType.BOOL
            else      -> BaseType.UNKNOWN
        }
    }
}

data class TypeQualifier(private val ident: Ident): AnyTypeNode() {
    override fun name(): String = ident.str()

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun qualifier(): PointerQualifier {
        return when (ident.str()) {
            "const"    -> PointerQualifier.CONST
            "volatile" -> PointerQualifier.VOLATILE
            "restrict" -> PointerQualifier.RESTRICT
            else       -> PointerQualifier.EMPTY
        }
    }
}

data class StorageClassSpecifier(private val ident: Ident): AnyTypeNode() {
    override fun name(): String = ident.str()

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun storageClass(): StorageClass {
        return when (ident.str()) {
            "typedef"  -> StorageClass.TYPEDEF
            "extern"   -> StorageClass.EXTERN
            "static"   -> StorageClass.STATIC
            "register" -> StorageClass.REGISTER
            "auto"     -> StorageClass.AUTO
            else       -> StorageClass.AUTO
        }
    }
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

    fun typeResolver(typeHolder: TypeHolder): BaseType {
        return StructDeclaration(name.str())
    }
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