package parser.nodes

import types.*
import tokenizer.*
import gen.IRCodeGenError
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


sealed interface UnaryOpType

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


sealed class Expression : Node() {
    protected var type: CType? = null

    abstract fun<T> accept(visitor: ExpressionVisitor<T>): T

    abstract fun resolveType(typeHolder: TypeHolder): CType

    protected inline fun<reified T: CType> memoize(closure: () -> T): T {
        if (type != null) {
            return type as T
        }
        type = closure()
        return type as T
    }
}

// https://port70.net/~nsz/c/c11/n1570.html#6.5.2.5
// 6.5.2.5 Compound literals
class CompoundLiteral(val typeName: TypeName, val initializerList: InitializerList) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize typeName.specifyType(typeHolder, listOf()).type.baseType()
    }
}

data class BinaryOp(val left: Expression, val right: Expression, val opType: BinaryOpType) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val leftType   = left.resolveType(typeHolder) as CPrimitive
        val rightType  = right.resolveType(typeHolder) as CPrimitive
        return@memoize leftType.interfere(rightType)
    }
}

data object EmptyExpression : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        throw IllegalStateException("Empty expression type is not resolved")
    }
}

class Conditional(val cond: Expression, val eTrue: Expression, val eFalse: Expression) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val typeTrue   = eTrue.resolveType(typeHolder) as CPrimitive
        val typeFalse  = eFalse.resolveType(typeHolder) as CPrimitive
        if (typeTrue == typeFalse && typeTrue == VOID) {
            return@memoize VOID
        }

        return@memoize typeTrue.interfere(typeFalse)
    }
}

sealed class AnyFunctionCall(val args: List<Expression>) : Expression() {
    abstract fun name(): String

    protected fun resolveParams(typeHolder: TypeHolder){
        val params = args.map { it.resolveType(typeHolder) }
        if (params.size != args.size) {
            throw TypeResolutionException("Function call of '${name()}' with unresolved types")
        }

        for (i in args.indices) {
            val argType = args[i].resolveType(typeHolder)
            if (argType == params[i]) {
                continue
            }

            throw TypeResolutionException("Function call of '${name()}' with wrong argument types")
        }
    }
}

class FuncPointerCall(val primary: Expression, args: List<Expression>) : AnyFunctionCall(args) {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return nameIdentifier().str()
    }

    private fun nameIdentifier(): Identifier = when (primary) {
        is UnaryOp -> (primary.primary as VarNode).nameIdent()
        is VarNode -> primary.nameIdent()
        is MemberAccess -> primary.ident
        is ArrowMemberAccess -> primary.ident
        else -> throw TypeResolutionException("Function call of '${primary}' with non-function type")
    }

    fun resolveFunctionType(typeHolder: TypeHolder): CFunPointerT = memoize {
        resolveParams(typeHolder)
        val functionType = primary.resolveType(typeHolder)
        if (functionType is AbstractCFunction) {
            return@memoize CFunPointerT(functionType, setOf())
        }
        if (functionType !is CFunPointerT) {
            throw TypeResolutionException("Function call with non-function type: $functionType")
        }
        return functionType
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        return resolveFunctionType(typeHolder).functionType.retType.baseType()
    }
}

class FunctionCall(val primary: VarNode, args: List<Expression>) : AnyFunctionCall(args) {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun name(): String {
        return nameIdentifier().str()
    }

    fun nameIdentifier(): Identifier {
        return primary.nameIdent()
    }

    private fun resolveFunctionType(typeHolder: TypeHolder): CBaseFunctionType = memoize {
        resolveParams(typeHolder)
        val functionType = typeHolder.getFunctionType(name()).type.baseType()
        if (functionType !is CBaseFunctionType) {
            throw TypeResolutionException("Function call of '${name()}' with non-function type")
        }
        return functionType
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        return resolveFunctionType(typeHolder).functionType.retType.baseType()
    }
}

sealed class Initializer : Expression()

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
        val types = initializers.map { it.resolveType(typeHolder) }

        val baseTypes = arrayListOf<CType>()
        for (i in initializers.indices) {
            baseTypes.add(types[i])
        }
        if (baseTypes.size == 1) {
            return@memoize baseTypes[0]
        } else {
            return@memoize InitializerType(baseTypes)
        }
    }

    fun length(): Int = initializers.size
}

class MemberAccess(val primary: Expression, val ident: Identifier) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    fun memberName(): String = ident.str()

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val structType = primary.resolveType(typeHolder)
        if (structType !is AnyStructType) {
            throw TypeResolutionException("Member access on non-struct type, but got $structType")
        }
        val field = structType.fieldIndex(ident.str())
        if (field != -1) {
            return@memoize structType.fields()[field].second.baseType()
        }
        throw TypeResolutionException("Field $ident not found in struct $structType")
    }
}

class ArrowMemberAccess(val primary: Expression, val ident: Identifier) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val structType = primary.resolveType(typeHolder)
        if (structType !is CPointer) {
            throw TypeResolutionException("Arrow member access on non-pointer type, but got $structType")
        }
        val baseType = structType.dereference()
        if (baseType !is AnyStructType) {
            throw TypeResolutionException("Arrow member access on non-struct type, but got $baseType")
        }
        val field = baseType.fieldIndex(ident.str())
        if (field != -1) {
            return@memoize baseType.fields()[field].second.baseType()
        }
        throw TypeResolutionException("Field $ident not found in struct $baseType")
    }
}

data class VarNode(private val str: Identifier) : Expression() {
    fun name(): String = str.str()
    fun nameIdent(): Identifier = str

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val varType = typeHolder.getVarTypeOrNull(str.str())
        if (varType != null) {
            return@memoize varType.type.baseType()
        }

        val enumType = typeHolder.findEnum(str.str())
        if (enumType != null) {
            return@memoize enumType
        }

        throw TypeResolutionException("Variable $str not found")
    }
}

data class StringNode(val literals: List<StringLiteral>) : Expression() {
    private val data by lazy {
        if (literals.all { it.unquote().isEmpty() }) {
            ""
        } else {
            literals.joinToString("") { it.unquote() } + "\\0"
        }
    }

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): SliceType = memoize {
        return@memoize SliceType(CHAR, data().length.toLong()) //ToDO restrict?????
    }

    fun data(): String = data
}

data class CharNode(val char: CharLiteral) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize CHAR
    }

    fun toInt(): Int {
        return char.data.toInt()
    }
}

data class NumNode(val number: Numeric) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        when (number.toNumberOrNull()) {
            is Int, is Byte -> INT
            is Long   -> LONG
            is ULong  -> ULONG
            is Float  -> FLOAT
            is Double -> DOUBLE
            else      -> throw TypeResolutionException("Unknown number type, but got ${number.str()}")
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
                when (primaryType) {
                    is CFunPointerT          -> primaryType
                    is CPointer              -> primaryType.dereference()
                    is CArrayType            -> primaryType.type.baseType()
                    is CUncompletedArrayType -> primaryType.elementType.baseType()
                    else -> throw TypeResolutionException("Dereference on non-pointer type: $primaryType")
                }
            }
            PrefixUnaryOpType.ADDRESS -> {
                CPointer(primaryType)
            }
            PrefixUnaryOpType.NOT -> {
                if (primaryType is CPointer) {
                    LONG //TODO UNSIGNED???
                } else {
                    primaryType
                }
            }
            PrefixUnaryOpType.NEG -> {
                if (primaryType is CPrimitive) {
                    primaryType
                } else {
                    throw TypeResolutionException("Negation on non-primitive type: $primaryType")
                }
            }
            PrefixUnaryOpType.INC,
            PrefixUnaryOpType.DEC,
            PrefixUnaryOpType.PLUS -> primaryType
            PrefixUnaryOpType.BIT_NOT -> {
                if (primaryType is CPrimitive) {
                    primaryType
                } else {
                    throw TypeResolutionException("Bitwise not on non-primitive type: $primaryType")
                }
            }

            else -> TODO("$opType")
        }

        return@memoize resolvedType
    }
}

data class ArrayAccess(val primary: Expression, val expr: Expression) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize when (val primaryType = primary.resolveType(typeHolder)) {
            is CArrayType -> primaryType.type.baseType()
            is CUncompletedArrayType -> primaryType.elementType.baseType()
            is CPointer     -> primaryType.dereference()
            else -> throw TypeResolutionException("Array access on non-array type: $primaryType")
        }
    }
}

data class SizeOf(val expr: Node) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize INT
    }

    fun constEval(typeHolder: TypeHolder): Int {
        when (expr) {
            is TypeName -> {
                val resolved = expr.specifyType(typeHolder, listOf()).type
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
        return@memoize typeName.specifyType(typeHolder, listOf()).type.baseType()
    }
}