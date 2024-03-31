package types

import kotlin.math.*
import parser.nodes.*


interface Type {
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

        fun common(types: List<Type>): Type = if (types.isEmpty()) UNKNOWN else types.reduce { a, b -> common(a, b) }
        fun common(a: Type, b: Type): Type {
            if (a is NumberType && b is NumberType) {
                if (a is IntType && b is IntType) {
                    return IntType(a.signed || b.signed, max(a.size, b.size))
                }
                return if (max(a.size, b.size) > 4) DOUBLE else FLOAT
            }
            return a
        }

        fun binop(l: Type, op: String, r: Type): BinopTypes = when (op) {
            "&&", "||" -> BinopTypes(Type.BOOL)
            "<<", ">>" -> BinopTypes(l.growToWord(), Type.INT)
            "==", "!=", "<", "<=", ">", ">=" -> {
                val common = Type.common(l, r)
                BinopTypes(common, common, Type.BOOL)
            }
            "&", "|", "^" -> BinopTypes(l.growToWord())
            "*", "/", "%" -> BinopTypes(l.growToWord())
            "+" -> when {
                l is ArrayType -> BinopTypes(l, Type.INT, l.elementType.ptr())
                l is PointerType -> BinopTypes(l, Type.INT, l)
                else -> BinopTypes(Type.common(l, r).growToWord())
            }
            "-" -> when {
                l is ArrayType -> BinopTypes(l, Type.INT, l.elementType.ptr())
                l is PointerType && r is PointerType -> BinopTypes(l, r, Type.INT)
                l is PointerType -> BinopTypes(l, Type.INT, l)
                else -> BinopTypes(Type.common(l, r).growToWord())
            }
            else -> TODO("BINOP '$op' $l, $r")
        }

        fun unop(op: String, r: Type): UnopTypes = when (op) {
            "!" -> UnopTypes(Type.BOOL)
            "~" -> UnopTypes(r.growToWord())
            else -> TODO("UNOP '$op' $r")
        }
    }
}

 
data class BinopTypes(val l: Type, val r: Type = l, val out: Type = l)

 
data class UnopTypes(val r: Type, val out: Type = r)

fun Type.growToWord(resolver: TypeResolver = UncachedTypeResolver): Type {
    val that = resolver.resolve(this)
    val res = when (that) {
        is BoolType -> Type.INT
        is IntType -> IntType(that.signed, max(that.size, 4))
        else -> that
    }
    //println("growToWord : $this[$that] -> $res")
    return res
}

fun Type.withSign(signed: Boolean) = when (this) {
    is IntType -> IntType(signed, size)
    else -> this
}

val Type.sign: Boolean?
    get() = when (this) {
        is IntType -> signed
        is NumberType -> true
        else -> null
    }

val Type.signed get() = sign ?: false
val Type.unsigned get() = !(sign ?: true)

val Type.elementType
    get() = when (this) {
        is BasePointerType -> this.elementType
        else -> Type.UNKNOWN_ELEMENT_TYPE
    }

fun Type.ptr(const: Boolean = false) = PointerType(this, const)

abstract class PrimType : Type

 
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
 
abstract class BaseReferenceableType() : Type

 
abstract class BasePointerType() : BaseReferenceableType() {
    abstract val elementType: Type
    abstract val actsAsPointer: Boolean
}

 
data class PointerType(override val elementType: Type, val const: Boolean) : BasePointerType() {
    //override fun toString(): String = "$type*"
    override val actsAsPointer: Boolean = true

    override fun toString(): String = "CPointer<$elementType>"

    companion object {
        const val POINTER_SIZE = 8
    }
}
 
data class ArrayType(override val elementType: Type, val numElements: Int) : BasePointerType() {
    val hasSubarrays get() = elementType is ArrayType
    // CAUSES PROBLEMS in initialization if there is something like array[8][23] and array[8][40] in the program
    //override val actsAsPointer: Boolean = !hasSubarrays || numElements == null
    override val actsAsPointer: Boolean = false
    override fun toString(): String = if (numElements != null) "$elementType[$numElements]" else "$elementType[]"
}
 
data class UnknownType(val reason: Any?) : PrimType() {
    override fun toString(): String = "UnknownFType($reason)"
}
data class FunctionType(val name: String, val retType: Type, val args: List<Parameter>, var variadic: Boolean = false) : Type {
    companion object {
        fun from(returnType: Type) = FunctionType("", returnType, arrayListOf())
        fun from(returnType: Type, args: List<Parameter>, isVariadic: Boolean) = FunctionType("", returnType, args, isVariadic)
    }
}