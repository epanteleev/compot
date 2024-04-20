package parser.nodes

import parser.nodes.visitors.ExpressionVisitor
import parser.nodes.visitors.NodeVisitor
import parser.nodes.visitors.Resolvable
import tokenizer.Ident
import tokenizer.Numeric
import tokenizer.StringLiteral
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
    },
    BIT_NOT {
        override fun toString(): String = "~"
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


abstract class Expression : Node(), Resolvable {
    protected var type: CType = CType.UNRESOlVED

    abstract fun<T> accept(visitor: ExpressionVisitor<T>): T
}

data class BinaryOp(val left: Expression, val right: Expression, val opType: BinaryOpType) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

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
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = type
}

class Conditional(val cond: Expression, val eTrue: Expression, val eFalse: Expression) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

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

data class FunctionCall(val primary: Expression, val args: List<Expression>) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return when (primary) {
            is VarNode -> primary.name()
            else -> throw IllegalStateException("Function call primary is not a VarNode, but got ${primary.javaClass.simpleName}")
        }
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val params = args.map { it.resolveType(typeHolder) }
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

        return typeHolder.getFunctionType(name())
    }
}

data class InitializerList(val initializers: List<Expression>) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

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
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val structType = primary.resolveType(typeHolder)
        if (structType !is CompoundType) {
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
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType { //TODO Copy-paste?!?
        if (type != CType.UNRESOlVED) {
            return type
        }

        val structType = primary.resolveType(typeHolder)
        if (structType !is CompoundType) {
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

data class VarNode(private val str: Ident) : Expression() {
    fun name(): String = str.str()

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        type = typeHolder[str.str()]
        return type
    }
}

data class StringNode(val str: StringLiteral) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }
        type = CPointerType(CType.CHAR)
        return type
    }
}

data class NumNode(val toLong: Numeric) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

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

data class UnaryOp(val primary: Expression, val opType: UnaryOpType) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val primaryType = primary.resolveType(typeHolder)
        if (opType is PrefixUnaryOpType) {
            type = when (opType) {
                PrefixUnaryOpType.DEREF -> {
                    primaryType as CPointerType
                    primaryType.dereference()
                }
                PrefixUnaryOpType.ADDRESS -> {
                    CPointerType(primaryType)
                }
                PrefixUnaryOpType.NOT -> {
                    TODO()
                }
                PrefixUnaryOpType.NEG -> {
                    if (primaryType.baseType() is CPrimitive) {
                        primaryType
                    } else {
                        CType.UNKNOWN
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
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        val primaryType = primary.resolveType(typeHolder)
        if (primaryType is CompoundType) {
            when (primaryType.baseType) {
                is CArrayType -> {
                    type = primaryType.baseType.type
                    return type
                }
                else -> {
                    type = CType.UNKNOWN
                    return type
                }
            }
        } else if (primaryType is CPointerType) {
            type = primaryType.dereference()
            return type
        }

        type = CType.UNKNOWN
        return type

    }
}

data class SizeOf(val expr: Node) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        expr as Resolvable
        type = expr.resolveType(typeHolder)
        return type
    }
}

data class Cast(val typeName: TypeName, val cast: Expression) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }

        type = typeName.resolveType(typeHolder)
        return type
    }
}