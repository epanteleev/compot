package parser.nodes

import types.*
import tokenizer.*
import parser.nodes.visitors.*


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
    private var type: CType = CType.UNRESOlVED

    abstract fun<T> accept(visitor: ExpressionVisitor<T>): T

    protected fun memoize(closure: () -> CType): CType {
        if (type != CType.UNRESOlVED) {
            return type
        }
        type = closure()
        return type
    }
}

data class BinaryOp(val left: Expression, val right: Expression, val opType: BinaryOpType) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val leftType   = left.resolveType(typeHolder)
        val rightType  = right.resolveType(typeHolder)
        val commonType = CType.interfereTypes(leftType, rightType)
        return@memoize commonType
    }
}

class EmptyExpression : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize { CType.UNKNOWN }
}

class Conditional(val cond: Expression, val eTrue: Expression, val eFalse: Expression) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val typeTrue   = eTrue.resolveType(typeHolder)
        val typeFalse  = eFalse.resolveType(typeHolder)
        val commonType = CType.interfereTypes(typeTrue, typeFalse)
        return@memoize commonType
    }
}

data class FunctionCall(val primary: Expression, val args: List<Expression>) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return nameIdentifier().str()
    }

    fun nameIdentifier(): Identifier {
        return when (primary) {
            is VarNode -> primary.nameIdent()
            else -> throw IllegalStateException("Function call primary is not a VarNode, but got ${primary.javaClass.simpleName}")
        }
    }

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val params = args.map { it.resolveType(typeHolder) }
        if (params.size != args.size) {
            return@memoize CType.UNKNOWN
        }

        for (i in args.indices) {
            val argType = args[i].resolveType(typeHolder)
            if (argType == params[i]) {
                continue
            }

            return@memoize CType.UNKNOWN
        }

        return@memoize typeHolder.getFunctionType(name())
    }
}

data class InitializerList(val initializers: List<Expression>) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val types      = initializers.map { it.resolveType(typeHolder) }
        val commonType = types.reduce { acc, type -> CType.interfereTypes(acc, type) }
        return@memoize commonType
    }
}

class MemberAccess(val primary: Expression, val ident: Identifier) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val structType = primary.resolveType(typeHolder)
        if (structType !is CompoundType) {
            return@memoize CType.UNKNOWN
        }
        val field = structType.fields.find { it.first == ident.str() }
        if (field != null) {
            return@memoize field.second
        }
        return@memoize CType.UNKNOWN
    }
}

class ArrowMemberAccess(val primary: Expression, val ident: Identifier) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val structType = primary.resolveType(typeHolder)
        if (structType !is CompoundType) {
            return@memoize CType.UNKNOWN
        }
        val field = structType.fields.find { it.first == ident.str() }
        if (field != null) {
            return@memoize field.second
        }
        return@memoize CType.UNKNOWN
    }
}

data class VarNode(private val str: Identifier) : Expression() {
    fun name(): String = str.str()
    fun nameIdent(): Identifier = str

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize typeHolder[str.str()]
    }
}

data class StringNode(val str: StringLiteral) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize CPointerType(CType.CHAR)
    }
}

data class NumNode(val toLong: Numeric) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize when (toLong.data) {
            is Int    -> CType.INT
            is Long   -> CType.LONG
            is Float  -> CType.FLOAT
            is Double -> CType.DOUBLE
            else      -> CType.UNKNOWN //TODO more types
        }
    }
}

data class UnaryOp(val primary: Expression, val opType: UnaryOpType) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val primaryType = primary.resolveType(typeHolder)
        if (opType !is PrefixUnaryOpType) {
            return@memoize primaryType
        }

        val resolvedType = when (opType) {
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
            PrefixUnaryOpType.INC,
            PrefixUnaryOpType.DEC,
            PrefixUnaryOpType.PLUS -> {
                primaryType
            }

            else -> TODO()
        }

        return@memoize resolvedType
    }
}

data class ArrayAccess(val primary: Expression, val expr: Expression) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)
    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val primaryType = primary.resolveType(typeHolder)
        if (primaryType is CompoundType) {
            when (primaryType.baseType) {
                is CArrayType -> {
                    return@memoize primaryType.baseType.type
                }
                else -> {
                    return@memoize CType.UNKNOWN
                }
            }
        } else if (primaryType is CPointerType) {
            return@memoize primaryType.dereference()
        }
        return@memoize CType.UNKNOWN
    }
}

data class SizeOf(val expr: Node) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        expr as Resolvable
        return@memoize expr.resolveType(typeHolder)
    }
}

data class Cast(val typeName: TypeName, val cast: Expression) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize typeName.resolveType(typeHolder)
    }
}