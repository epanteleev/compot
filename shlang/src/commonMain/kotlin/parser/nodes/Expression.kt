package parser.nodes

import gen.IRCodeGenError
import ir.value.IntegerConstant
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


abstract class Expression : Node() {
    private var type: CType = CType.UNRESOlVED

    abstract fun<T> accept(visitor: ExpressionVisitor<T>): T

    abstract fun resolveType(typeHolder: TypeHolder): CType

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

object EmptyExpression : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize { CType.UNKNOWN }
}

class Conditional(val cond: Expression, val eTrue: Expression, val eFalse: Expression) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val typeTrue   = eTrue.resolveType(typeHolder)
        val typeFalse  = eFalse.resolveType(typeHolder)
        if (typeTrue == typeFalse && typeTrue == CType.VOID) {
            return@memoize CType.VOID
        }
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
            else -> throw IllegalStateException("Function call primary is not a VarNode, but got ${primary::class.simpleName}")
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

        val functionType = typeHolder.getFunctionType(name()) as CFunctionType
        return@memoize functionType.functionType.retType
    }
}

abstract class Initializer : Expression()

class SingleInitializer(val expr: Expression) : Initializer() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize expr.resolveType(typeHolder)
    }
}

class DesignationInitializer(val designation: Designation, val initializer: Expression) : Initializer() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize initializer.resolveType(typeHolder)
    }
}

class InitializerList(val initializers: List<Initializer>) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val types      = initializers.map { it.resolveType(typeHolder) }
        val commonType = types.reduce { acc, type ->
            CType.interfereTypes(acc, type)
        }
        if (isSameType(types)) {
            val base = CArrayBaseType(commonType, types.size.toLong())
            return@memoize CArrayType(base, emptyList())
        }
        val struct = StructBaseType("initializer")
        for (i in initializers.indices) {
            struct.addField("field$i", types[i])
        }
        return@memoize CStructType(struct, emptyList())
    }

    private fun isSameType(types: List<CType>): Boolean {
        return types.all { it == types.first() }
    }
}

class MemberAccess(val primary: Expression, val ident: Identifier) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    fun memberName(): String = ident.str()

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val structType = primary.resolveType(typeHolder)
        if (structType !is CBaseStructType) {
            return@memoize CType.UNKNOWN
        }
        val field = structType.fieldIndex(ident.str())
        if (field != -1) {
            return@memoize structType.fields()[field].second
        }
        return@memoize CType.UNKNOWN
    }
}

class ArrowMemberAccess(val primary: Expression, val ident: Identifier) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val structType = primary.resolveType(typeHolder)
        if (structType !is CPointerType) {
            return@memoize CType.UNKNOWN
        }
        val baseType = structType.dereference()
        if (baseType !is CBaseStructType) {
            return@memoize CType.UNKNOWN
        }
        val field = baseType.fieldIndex(ident.str())
        if (field != -1) {
            return@memoize baseType.fields()[field].second
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
        return@memoize CPointerType(CType.CHAR, listOf()) //ToDO restrict?????
    }
}

data class CharNode(val char: CharLiteral) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize CType.CHAR
    }

    fun toInt(): Int {
        return char.data.toInt()
    }
}

data class NumNode(val number: Numeric) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val num = number.toNumberOrNull()

        return@memoize when (num) {
            is Int, is Byte -> CType.INT
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
                CPointerType(primaryType, listOf())
            }
            PrefixUnaryOpType.NOT -> {
                if (primaryType is CPointerType) {
                    CType.LONG //TODO UNSIGNED
                } else {
                    primaryType
                }
            }
            PrefixUnaryOpType.NEG -> {
                if (primaryType is CPrimitiveType) {
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
        if (primaryType is CArrayType) {
            return@memoize primaryType.element()
        } else if (primaryType is CPointerType) {
            return@memoize primaryType.dereference()
        }
        return@memoize CType.UNKNOWN
    }
}

data class SizeOf(val expr: Node) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize CType.INT
    }

    fun constEval(typeHolder: TypeHolder): Int {
        when (expr) {
            is TypeName -> {
                val resolved = expr.specifyType(typeHolder)
                return resolved.size()
            }
            is VarNode -> {
                val resolved = expr.resolveType(typeHolder)
                return resolved.size()
            }
            else -> throw IRCodeGenError("Unknown sizeOf expression, expr=${expr}")
        }
    }
}

data class Cast(val typeName: TypeName, val cast: Expression) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize typeName.specifyType(typeHolder)
    }
}