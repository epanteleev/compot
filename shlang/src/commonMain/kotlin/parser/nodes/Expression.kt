package parser.nodes

import types.*
import typedesc.*
import tokenizer.tokens.*
import common.assertion
import parser.LineAgnosticAstPrinter
import parser.nodes.BinaryOpType.AND
import parser.nodes.BinaryOpType.OR
import parser.nodes.BinaryOpType.COMMA
import parser.nodes.visitors.*
import tokenizer.Position


sealed class Expression {
    protected var type: CType? = null

    abstract fun begin(): Position
    abstract fun<T> accept(visitor: ExpressionVisitor<T>): T
    abstract fun resolveType(typeHolder: TypeHolder): CType

    protected fun convertToPrimitive(type: CType): CPrimitive? = when (type) {
        is CPrimitive -> type
        is AnyCArrayType -> type.asPointer()
        is AnyCFunctionType -> type.asPointer()
        else -> null
    }

    protected fun convertToPointer(type: CType): CPointer? = when (type) {
        is CPointer      -> type
        is AnyCArrayType -> type.asPointer()
        else -> null
    }

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
    override fun begin(): Position = typeName.begin()

    fun typeDesc(typeHolder: TypeHolder): TypeDesc {
        val type = typeName.specifyType(typeHolder).typeDesc
        val ctype = type.cType()
        if (ctype is CUncompletedArrayType) {
            return TypeDesc.from(CArrayType(ctype.element(), initializerList.length().toLong()))
        }

        return type
    }

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize typeDesc(typeHolder).cType()
    }
}

data class BinaryOp(val left: Expression, val right: Expression, val opType: BinaryOpType) : Expression() {
    override fun begin(): Position = left.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        when (opType) {
            OR, AND -> return@memoize BOOL
            COMMA -> return@memoize right.resolveType(typeHolder)
            BinaryOpType.SHL,
            BinaryOpType.SHR -> return@memoize left.resolveType(typeHolder)
            else -> {}
        }
        val l = left.resolveType(typeHolder)
        val r = right.resolveType(typeHolder)
        if (l == r) {
            return@memoize l
        }

        val leftType = convertToPrimitive(l)
            ?: throw TypeResolutionException("Binary operation on non-primitive type '$l': '${LineAgnosticAstPrinter.print(left)}'", begin())

        val rightType = convertToPrimitive(r)
            ?: throw TypeResolutionException("Binary operation on non-primitive type '$r': '${LineAgnosticAstPrinter.print(right)}'", begin())

        val resultType = leftType.interfere(rightType) ?:
            throw TypeResolutionException("Binary operation '$opType' on incompatible types: $leftType and $rightType in ${left.begin()}'", begin())
        return@memoize resultType
    }
}

class EmptyExpression(private val where: Position) : Expression() {
    override fun begin(): Position = where
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        throw IllegalStateException("Empty expression type is not resolved")
    }
}

class Conditional(val cond: Expression, val eTrue: Expression, val eFalse: Expression) : Expression() {
    override fun begin(): Position = cond.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val typeTrue  = eTrue.resolveType(typeHolder)
        val typeFalse = eFalse.resolveType(typeHolder)

        if (typeTrue is VOID || typeFalse is VOID) {
            return@memoize VOID
        }

        if (typeTrue is AnyCStructType && typeFalse is AnyCStructType && typeTrue == typeFalse) {
            return@memoize typeTrue
        }

        val cvtTypeTrue  = convertToPrimitive(typeTrue)
            ?: throw TypeResolutionException("Conditional with non-primitive types: $typeTrue and $typeFalse", begin())
        val cvtTypeFalse = convertToPrimitive(typeFalse)
            ?: throw TypeResolutionException("Conditional with non-primitive types: $typeTrue and $typeFalse", begin())

        if (cvtTypeTrue == cvtTypeFalse) {
            return@memoize cvtTypeTrue
        }

        return@memoize cvtTypeTrue.interfere(cvtTypeFalse) ?:
            throw TypeResolutionException("Conditional with incompatible types: $cvtTypeTrue and $cvtTypeFalse: '${LineAgnosticAstPrinter.print(this)}'", begin())
    }
}

class FunctionCall(val primary: Expression, val args: List<Expression>) : Expression() {
    override fun begin(): Position = primary.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    private fun resolveParams(typeHolder: TypeHolder){
        val params = args.map { it.resolveType(typeHolder) }
        if (params.size != args.size) {
            throw TypeResolutionException("Function call of '${LineAgnosticAstPrinter.print(primary)}' with unresolved types", begin())
        }

        for (i in args.indices) {
            val argType = args[i].resolveType(typeHolder)
            if (argType == params[i]) {
                continue
            }
            throw TypeResolutionException("Function call of '${LineAgnosticAstPrinter.print(primary)}' with wrong argument types", begin())
        }
    }

    private fun resolveFunctionType0(typeHolder: TypeHolder): CPointer {
        val functionType = primary.resolveType(typeHolder)
        if (functionType is AbstractCFunction) {
            return CPointer(functionType, setOf())
        }
        if (functionType !is CPointer) {
            throw TypeResolutionException("Function call with non-function type: $functionType", begin())
        }
        return functionType
    }

    fun functionType(typeHolder: TypeHolder): AnyCFunctionType {
        resolveParams(typeHolder)
        val functionType = if (primary !is VarNode) {
            resolveFunctionType0(typeHolder)
        } else {
            typeHolder.getFunctionType(primary.name()).typeDesc.cType()
        }
        if (functionType is CPointer) {
            return functionType.dereference(typeHolder) as AbstractCFunction
        }
        if (functionType !is CFunctionType) {
            throw TypeResolutionException("Function call of '' with non-function type", begin())
        }

        return functionType
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        return functionType(typeHolder).retType().cType()
    }
}

class MemberAccess(val primary: Expression, val fieldName: Identifier) : Expression() {
    override fun begin(): Position = primary.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    fun memberName(): String = fieldName.str()

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val structType = primary.resolveType(typeHolder)
        if (structType !is AnyCStructType) {
            throw TypeResolutionException("Member access on non-struct type, but got $structType", begin())
        }

        val fieldDesc = structType.fieldByNameOrNull(memberName()) ?: throw TypeResolutionException("Field $fieldName not found in struct $structType", begin())
        return@memoize fieldDesc.cType()
    }
}

class ArrowMemberAccess(val primary: Expression, private val ident: Identifier) : Expression() {
    override fun begin(): Position = primary.begin()
    fun fieldName(): String = ident.str()

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val ty = primary.resolveType(typeHolder)
        val structType = convertToPointer(ty)
            ?: throw TypeResolutionException("Arrow member access on non-pointer type, but got $ty", begin())

        val baseType = structType.dereference(typeHolder)
        if (baseType !is AnyCStructType) {
            throw TypeResolutionException("Arrow member access on non-struct type, but got $baseType", begin())
        }

        val fieldDesc = baseType.fieldByNameOrNull(fieldName()) ?: throw TypeResolutionException("Field $ident not found in struct $baseType", begin())
        return@memoize fieldDesc.cType()
    }
}

data class VarNode(private val str: Identifier) : Expression() {
    override fun begin(): Position = str.position()
    fun name(): String = str.str()
    fun nameIdent(): Identifier = str

    fun position(): Position = str.position()

    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val varType = typeHolder.getVarTypeOrNull(str.str())
        if (varType != null) {
            return@memoize varType.typeDesc.cType()
        }

        return@memoize typeHolder.findEnum(str.str()) ?: throw TypeResolutionException("Variable '$str' not found", begin())
    }
}

data class StringNode(val literals: List<StringLiteral>) : Expression() {
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

    override fun resolveType(typeHolder: TypeHolder): CStringLiteral = memoize {
        if (data.isEmpty()) {
            return@memoize CStringLiteral(TypeDesc.from(CHAR), 1)
        }

        return@memoize CStringLiteral(TypeDesc.from(CHAR), length().toLong())
    }

    fun length(): Int = data.length + 1

    fun isNotEmpty(): Boolean = data.isNotEmpty()

    fun data(): String = data
}

data class CharNode(val char: CharLiteral) : Expression() {
    override fun begin(): Position = char.position()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize CHAR
    }

    fun toByte(): Byte {
        return char.code()
    }
}

data class NumNode(val number: PPNumber) : Expression() {
    override fun begin(): Position = number.position()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CPrimitive = number.type
}

data class UnaryOp(val primary: Expression, val opType: UnaryOpType) : Expression() {
    override fun begin(): Position = primary.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val primaryType = primary.resolveType(typeHolder)
        if (opType !is PrefixUnaryOpType) {
            return@memoize convertToPrimitive(primaryType)
                ?: throw TypeResolutionException("Unary operation '${opType}' on non-primitive type: $primaryType", begin())
        }

        return@memoize when (opType) {
            PrefixUnaryOpType.DEREF -> when (primaryType) {
                is CPointer      -> primaryType.dereference(typeHolder)
                is AnyCArrayType -> primaryType.element().cType()
                else -> throw TypeResolutionException("Dereference on non-pointer type: $primaryType", begin())
            }
            PrefixUnaryOpType.ADDRESS -> CPointer(primaryType)
            PrefixUnaryOpType.NOT -> INT
            PrefixUnaryOpType.NEG,
            PrefixUnaryOpType.INC,
            PrefixUnaryOpType.DEC,
            PrefixUnaryOpType.PLUS,
            PrefixUnaryOpType.BIT_NOT -> convertToPrimitive(primaryType)
                ?: throw TypeResolutionException("Unary operation '${opType}' on non-primitive type: $primaryType", begin())
        }
    }
}

data class ArrayAccess(val primary: Expression, val expr: Expression) : Expression() {
    override fun begin(): Position = primary.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize when (val primaryType = primary.resolveType(typeHolder)) {
            is AnyCArrayType -> primaryType.element().cType()
            is CPointer -> primaryType.dereference(typeHolder)
            is CPrimitive -> {
                val expressionType = expr.resolveType(typeHolder)
                val exprPointer = convertToPointer(expressionType)
                    ?: throw TypeResolutionException("Array access with non-pointer type: $expressionType", begin())
                exprPointer.dereference(typeHolder)
            }
            else -> throw TypeResolutionException("Array access on non-array type: $primaryType", begin())
        }
    }
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
    override fun constEval(typeHolder: TypeHolder): Int {
        val resolved = expr.resolveType(typeHolder)
        if (resolved !is CompletedType) {
            throw TypeResolutionException("sizeof on uncompleted type: $resolved", begin())
        }

        return resolved.size()
    }
}

data class SizeOf(val expr: SizeOfParam) : Expression() {
    override fun begin(): Position = expr.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize LONG
    }

    fun constEval(typeHolder: TypeHolder): Int = expr.constEval(typeHolder)
}

data class Cast(val typeName: TypeName, val cast: Expression) : Expression() {
    override fun begin(): Position = typeName.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize typeName.specifyType(typeHolder).typeDesc.cType()
    }
}

class BuiltinVaArg(val assign: Expression, val typeName: TypeName) : Expression() {
    override fun begin(): Position = assign.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize typeName.specifyType(typeHolder).typeDesc.cType()
    }
}

class BuiltinVaStart(val vaList: Expression, val param: Expression) : Expression() {
    override fun begin(): Position = vaList.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize VOID
    }
}

class BuiltinVaEnd(val vaList: Expression) : Expression() {
    override fun begin(): Position = vaList.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize VOID
    }
}

class BuiltinVaCopy(val dest: Expression, val src: Expression) : Expression() {
    override fun begin(): Position = dest.begin()
    override fun<T> accept(visitor: ExpressionVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize VOID
    }
}