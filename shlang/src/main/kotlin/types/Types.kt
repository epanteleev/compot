package types

import kotlin.math.*
import parser.nodes.*


interface CType: TypeProperty {
    val isNumericIntegral: Boolean get() = when (this) {
        CHAR, SHORT, INT, LONG, UCHAR, USHORT, UINT, ULONG -> true
        else -> false
    }
    val isNumeric: Boolean get() = isNumericIntegral || when (this) {
        FLOAT, DOUBLE -> true
        else -> false
    }
    val isInt8Bits: Boolean get() = this == CHAR || this == UCHAR
    val isInt16Bits: Boolean get() = this == SHORT || this == USHORT
    val isInt32Bits: Boolean get() = this == INT || this == UINT
    val isInt64Bits: Boolean get() = this == LONG || this == ULONG

    val is32Bits: Boolean get() = isInt32Bits || this == FLOAT
    val is64Bits: Boolean get() = isInt64Bits || this == DOUBLE

    companion object {
        val BOOL = BoolType
        val VOID = IntType(true, 0)

        val CHAR = IntType(true, 1)
        val SHORT = IntType(true, 2)
        val INT = IntType(true, 4)
        val LONG = IntType(true, 8)

        val UCHAR = IntType(false, 1)
        val USHORT = IntType(false, 2)
        val UINT = IntType(false, 4)
        val ULONG = IntType(false, 8)

        val FLOAT = FloatType
        val DOUBLE = DoubleType

        val VOID_PTR = PointerType(VOID, false)
        val CHAR_PTR = PointerType(CHAR, false)

        val UNKNOWN = UnknownType("unknown")
        val UNKNOWN_TYPEDEF = UnknownType("unknown_typedef")
        val UNKNOWN_ELEMENT_TYPE = UnknownType("unknown_element_type")
        val UNRESOLVED = UnknownType("unresolved")

        fun common(types: List<CType>): CType = if (types.isEmpty()) UNKNOWN else types.reduce { a, b -> common(a, b) }
        fun common(a: CType, b: CType): CType {
            if (a is NumberType && b is NumberType) {
                if (a is IntType && b is IntType) {
                    return IntType(a.signed || b.signed, max(a.size, b.size))
                }
                return if (max(a.size, b.size) > 4) DOUBLE else FLOAT
            }
            return a
        }

        fun binop(l: CType, op: String, r: CType): BinopTypes = when (op) {
            "&&", "||" -> BinopTypes(CType.BOOL)
            "<<", ">>" -> BinopTypes(l.growToWord(), CType.INT)
            "==", "!=", "<", "<=", ">", ">=" -> {
                val common = CType.common(l, r)
                BinopTypes(common, common, CType.BOOL)
            }
            "&", "|", "^" -> BinopTypes(l.growToWord())
            "*", "/", "%" -> BinopTypes(l.growToWord())
            "+" -> when {
                l is ArrayType -> BinopTypes(l, CType.INT, l.elementType.ptr())
                l is PointerType -> BinopTypes(l, CType.INT, l)
                else -> BinopTypes(CType.common(l, r).growToWord())
            }
            "-" -> when {
                l is ArrayType -> BinopTypes(l, CType.INT, l.elementType.ptr())
                l is PointerType && r is PointerType -> BinopTypes(l, r, CType.INT)
                l is PointerType -> BinopTypes(l, CType.INT, l)
                else -> BinopTypes(CType.common(l, r).growToWord())
            }
            else -> TODO("BINOP '$op' $l, $r")
        }

        fun unop(op: String, r: CType): UnopTypes = when (op) {
            "!" -> UnopTypes(CType.BOOL)
            "~" -> UnopTypes(r.growToWord())
            else -> TODO("UNOP '$op' $r")
        }
    }
}

 
data class BinopTypes(val l: CType, val r: CType = l, val out: CType = l)

 
data class UnopTypes(val r: CType, val out: CType = r)

fun CType.growToWord(resolver: TypeResolver = UncachedTypeResolver): CType {
    val that = resolver.resolve(this)
    val res = when (that) {
        is BoolType -> CType.INT
        is IntType -> IntType(that.signed, max(that.size, 4))
        else -> that
    }
    //println("growToWord : $this[$that] -> $res")
    return res
}

fun CType.withSign(signed: Boolean) = when (this) {
    is IntType -> IntType(signed, size)
    else -> this
}

val CType.sign: Boolean?
    get() = when (this) {
        is IntType -> signed
        is NumberType -> true
        else -> null
    }

val CType.signed get() = sign ?: false
val CType.unsigned get() = !(sign ?: true)

val CType.elementType
    get() = when (this) {
        is BasePointerType -> this.elementType
        else -> CType.UNKNOWN_ELEMENT_TYPE
    }

fun CType.ptr(const: Boolean = false) = PointerType(this, const)

abstract class PrimType : CType

 
abstract class NumberType : PrimType() {
    abstract val size: Int
}

 
object BoolType : PrimType() {
    override fun toString(): String = "Bool"
}

 
data class IntType(val signed: Boolean, override val size: Int) : NumberType() {
    override fun toString(): String = when (size) {
        0 -> "void"
        1 -> if (this.signed) "char" else "unsigned char"
        2 -> if (this.signed) "short" else "unsigned short"
        4 -> if (this.signed) "int" else "unsigned int"
        8 -> if (this.signed) "long" else "unsigned long"
        else -> TODO("IntFType")
    }
}

 
abstract class FloatingType : NumberType()

 
object FloatType : FloatingType() {
    override val size: Int = 4
    override fun toString(): String = "float"
}

 
object DoubleType : FloatingType() {
    override val size: Int = 8
    override fun toString(): String = "double"
}
 
abstract class BaseReferenceableType() : CType

 
abstract class BasePointerType() : BaseReferenceableType() {
    abstract val elementType: CType
    abstract val actsAsPointer: Boolean
}

 
data class PointerType(override val elementType: CType, val const: Boolean) : BasePointerType() {
    //override fun toString(): String = "$type*"
    override val actsAsPointer: Boolean = true

    override fun toString(): String = "CPointer<$elementType>"

    companion object {
        const val POINTER_SIZE = 8
    }
}
 
data class ArrayType(override val elementType: CType, val numElements: Int) : BasePointerType() {
    val hasSubarrays get() = elementType is ArrayType
    // CAUSES PROBLEMS in initialization if there is something like array[8][23] and array[8][40] in the program
    //override val actsAsPointer: Boolean = !hasSubarrays || numElements == null
    override val actsAsPointer: Boolean = false
    override fun toString(): String = if (numElements != null) "$elementType[$numElements]" else "$elementType[]"
}
 
data class UnknownType(val reason: Any?) : PrimType() {
    override fun toString(): String = "UnknownFType($reason)"
}
data class FunctionType(val name: String, val retType: CType, val args: List<Parameter>, var variadic: Boolean = false) : CType {
    companion object {
        fun from(returnType: CType) = FunctionType("", returnType, arrayListOf())
        fun from(returnType: CType, args: List<Parameter>, isVariadic: Boolean) = FunctionType("", returnType, args, isVariadic)
    }
}