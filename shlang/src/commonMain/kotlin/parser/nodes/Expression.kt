package parser.nodes

import types.*
import typedesc.*
import tokenizer.tokens.*
import gen.IRCodeGenError
import parser.LineAgnosticAstPrinter
import parser.nodes.visitors.*
import tokenizer.Position


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

    fun typeDesc(typeHolder: TypeHolder): TypeDesc {
        return typeName.specifyType(typeHolder, listOf()).type
    }

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize typeDesc(typeHolder).cType()
    }
}

data class BinaryOp(val left: Expression, val right: Expression, val opType: BinaryOpType) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val leftType  = when (val l = left.resolveType(typeHolder)) {
            is CArrayType            -> l.asPointer()
            is CUncompletedArrayType -> l.asPointer()
            else -> l
        }
        if (leftType !is CPrimitive) {
            throw TypeResolutionException("Binary operation on non-primitive type: '${LineAgnosticAstPrinter.print(left)}'")
        }

        val rightType = when (val r = right.resolveType(typeHolder)) {
            is CArrayType            -> r.asPointer()
            is CUncompletedArrayType -> r.asPointer()
            else -> r
        }
        if (rightType !is CPrimitive) {
            throw TypeResolutionException("Binary operation on non-primitive type: '${LineAgnosticAstPrinter.print(right)}'")
        }

        return@memoize leftType.interfere(typeHolder, rightType)
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
        if (eTrue is StringNode) {
            return@memoize CPointer(CHAR)
        }
        val typeTrue   = eTrue.resolveType(typeHolder)
        val typeFalse  = eFalse.resolveType(typeHolder)
        if (typeTrue == typeFalse && typeTrue == VOID) {
            return@memoize VOID
        }

        val cvtTypeTrue = when (typeTrue) {
            is CPrimitive     -> typeTrue
            is CStringLiteral -> typeTrue.asPointer()
            else -> throw TypeResolutionException("Conditional true branch with non-primitive type: $typeTrue")
        }

        val cvtTypeFalse = when (typeFalse) {
            is CPrimitive     -> typeFalse
            is CStringLiteral -> typeFalse.asPointer()
            else -> throw TypeResolutionException("Conditional false branch with non-primitive type: $typeFalse")
        }

        return@memoize cvtTypeTrue.interfere(typeHolder, cvtTypeFalse)
    }
}

class FunctionCall(val primary: Expression, val args: List<Expression>) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    private fun resolveParams(typeHolder: TypeHolder){
        val params = args.map { it.resolveType(typeHolder) }
        if (params.size != args.size) {
            throw TypeResolutionException("Function call of '${LineAgnosticAstPrinter.print(primary)}' with unresolved types")
        }

        for (i in args.indices) {
            val argType = args[i].resolveType(typeHolder)
            if (argType == params[i]) {
                continue
            }
            throw TypeResolutionException("Function call of '${LineAgnosticAstPrinter.print(primary)}' with wrong argument types")
        }
    }

    private fun resolveFunctionType0(typeHolder: TypeHolder): CPointer {
        val functionType = primary.resolveType(typeHolder)
        if (functionType is AbstractCFunction) {
            return CPointer(functionType, setOf())
        }
        if (functionType !is CPointer) {
            throw TypeResolutionException("Function call with non-function type: $functionType")
        }
        return functionType
    }

    fun functionType(typeHolder: TypeHolder): AnyCFunctionType {
        resolveParams(typeHolder)
        val functionType = if (primary !is VarNode) {
            resolveFunctionType0(typeHolder)
        } else {
            typeHolder.getFunctionType(primary.name()).type.cType()
        }
        if (functionType is CPointer) {
            return functionType.dereference(typeHolder) as AbstractCFunction
        }
        if (functionType !is CFunctionType) {
            throw TypeResolutionException("Function call of '' with non-function type")
        }

        return functionType
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        return functionType(typeHolder).retType().cType()
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
        if (structType !is AnyCStructType) {
            throw TypeResolutionException("Member access on non-struct type, but got $structType")
        }

        return@memoize structType.field(ident.str()) ?: throw TypeResolutionException("Field $ident not found in struct $structType")
    }
}

class ArrowMemberAccess(val primary: Expression, private val ident: Identifier) : Expression() {
    fun fieldName(): String = ident.str()

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val structType = when(val ty = primary.resolveType(typeHolder)) {
            is CArrayType            -> ty.asPointer()
            is CUncompletedArrayType -> ty.asPointer()
            else -> ty
        }
        if (structType !is CPointer) {
            throw TypeResolutionException("Arrow member access on non-pointer type, but got $structType")
        }
        val baseType = structType.dereference(typeHolder)
        if (baseType !is AnyCStructType) {
            throw TypeResolutionException("Arrow member access on non-struct type, but got $baseType")
        }

        return@memoize baseType.field(ident.str()) ?: throw TypeResolutionException("Field $ident not found in struct $baseType")
    }
}

data class VarNode(private val str: Identifier) : Expression() {
    fun name(): String = str.str()
    fun nameIdent(): Identifier = str

    fun position(): Position = str.position()

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val varType = typeHolder.getVarTypeOrNull(str.str())
        if (varType != null) {
            return@memoize varType.type.cType()
        }

        return@memoize typeHolder.findEnum(str.str()) ?: throw TypeResolutionException("Variable '$str' not found")
    }
}

data class StringNode(val literals: List<StringLiteral>) : Expression() {
    private val data by lazy {
        if (literals.all { it.unquote().isEmpty() }) {
            "\\0"
        } else {
            literals.joinToString("", postfix = "\\0") { it.unquote() }
        }
    }

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CStringLiteral = memoize {
        return@memoize CStringLiteral(TypeDesc.from(CHAR), data().length.toLong()) //ToDO restrict?????
    }

    fun data(): String = data
}

data class CharNode(val char: CharLiteral) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize CHAR
    }

    fun toInt(): Int {
        return char.data.code.toInt()
    }
}

data class NumNode(val number: PPNumber) : Expression() {
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
            PrefixUnaryOpType.DEREF -> when (primaryType) {
                is CPointer              -> primaryType.dereference(typeHolder)
                is CArrayType            -> primaryType.type.cType()
                is CUncompletedArrayType -> primaryType.elementType.cType()
                else -> throw TypeResolutionException("Dereference on non-pointer type: $primaryType")
            }
            PrefixUnaryOpType.ADDRESS -> CPointer(primaryType)
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
            is CArrayType            -> primaryType.type.cType()
            is CUncompletedArrayType -> primaryType.elementType.cType()
            is CPointer     -> primaryType.dereference(typeHolder)
            is CPrimitive   -> {
                val exprType = when (val e = expr.resolveType(typeHolder)) {
                    is CArrayType            -> e.asPointer()
                    is CUncompletedArrayType -> e.asPointer()
                    else -> e as CPointer
                }
                primaryType.interfere(typeHolder, exprType)
            }
            else -> throw TypeResolutionException("Array access on non-array type: $primaryType")
        }
    }
}

data class SizeOf(val expr: Node) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize INT
    }

    fun constEval(typeHolder: TypeHolder): Int = when (expr) {
        is TypeName -> {
            val resolved = expr.specifyType(typeHolder, listOf()).type
            resolved.size()
        }
        is VarNode -> {
            val resolved = expr.resolveType(typeHolder)
            resolved.size()
        }
        else -> throw IRCodeGenError("Unknown sizeOf expression, expr=${expr}")
    }
}

data class Cast(val typeName: TypeName, val cast: Expression) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize typeName.specifyType(typeHolder, listOf()).type.cType()
    }
}

data class IdentNode(private val str: Identifier) : Expression() {
    fun str(): String = str.str()
    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }
}

class BuiltinExpression(val name: String, val assign: Expression, val typeName: TypeName) : Expression() {
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize typeName.specifyType(typeHolder, listOf()).type.cType()
    }
}