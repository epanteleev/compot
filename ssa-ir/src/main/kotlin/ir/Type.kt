package ir

import java.lang.StringBuilder

data class TypeErrorException(override val message: String): Exception(message)

enum class TypeKind {
    U1,
    U8,
    U16,
    U32,
    U64,
    I8,
    I16,
    I32,
    I64,
    VOID,
    UNDEFINED;

    override fun toString(): String {
        val string = when(this) {
            U1        -> "u1"
            U8        -> "u8"
            U16       -> "u16"
            U32       -> "u32"
            U64       -> "u64"
            I8        -> "i8"
            I16       -> "i16"
            I32       -> "i32"
            I64       -> "i64"
            VOID      -> "void"
            UNDEFINED -> "undef"
        }

        return string
    }
}

data class Type(val kind: TypeKind, val indirection: Int) {
    fun withIndirection(indirection: Int): Type {
        return Type(kind, indirection)
    }

    fun ptr(): Type {
        return Type(kind, indirection + 1)
    }

    fun dereferenceOrNull(): Type? {
        val newInd = indirection - 1
        if (newInd < 0) {
            return null
        } else {
            return Type(kind, newInd)
        }
    }

    fun dereference(): Type {
        return dereferenceOrNull() ?: throw RuntimeException("Can't dereference this type $this")
    }

    fun isPointer(): Boolean {
        return indirection > 0
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for (i in 0 until indirection) {
            builder.append('*')
        }
        builder.append(kind)
        return builder.toString()
    }

    fun isArithmetic(): Boolean {
        if (indirection != 0) {
            return false
        }
        val isArithm = when (kind) {
            TypeKind.U1, TypeKind.UNDEFINED, TypeKind.VOID -> false
            else                                           -> true
        }
        return isArithm
    }

    fun isUnsigned(): Boolean {
        return when (kind) {
            TypeKind.U8, TypeKind.U16, TypeKind.U32, TypeKind.U64 -> true
            else                                                  -> false
        }
    }

    fun isSigned(): Boolean {
        return when (kind) {
            TypeKind.I8, TypeKind.I16, TypeKind.I32, TypeKind.I64 -> true
            else                                                  -> false
        }
    }

    /** Return size in bytes of given type. */
    fun size(): Int {
        if (isPointer()) {
            return 8
        }

        val s = when(kind) {
            TypeKind.U1 -> throw TypeErrorException("Cannot get size of $kind")
            TypeKind.U8, TypeKind.I8   -> 1
            TypeKind.U16, TypeKind.I16 -> 2
            TypeKind.U32, TypeKind.I32 -> 4
            TypeKind.U64, TypeKind.I64 -> 8
            else -> throw TypeErrorException("Cannot get size of type: $kind")
        }
        return s
    }

    fun bitmask(): Long {
        if (isPointer()) {
            return -1L // 0xffff_ffff_ffff_ffff
        }

        val mask = when (kind) {
            TypeKind.U1                -> 1
            TypeKind.U8,  TypeKind.I8  -> 0xff
            TypeKind.U16, TypeKind.I16 -> 0xffff
            TypeKind.U32, TypeKind.I32 -> 0xffff_ffff
            TypeKind.U64, TypeKind.I64 -> -1L
            else -> throw TypeErrorException("Cannot get bitmask: $kind")
        }
        return mask
    }

    fun isConvertableTo(ty: Type): Boolean {
        if (this === ty) {
            return true
        }
        if (indirection != ty.indirection) {
            return false
        }
        val isDifferentSign = (isSigned() && ty.isUnsigned()) || (isUnsigned() && ty.isSigned())
        if (isDifferentSign) {
            return false
        }

        val tySize = ty.size()
        return tySize >= size()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (javaClass != other?.javaClass) return false
        other as Type

        return kind == other.kind && indirection == other.indirection
    }

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + indirection
        return result
    }

    companion object {
        val U1  = Type(TypeKind.U1, 0)

        val U8  = Type(TypeKind.U8, 0)
        val U16 = Type(TypeKind.U16, 0)
        val U32 = Type(TypeKind.U32, 0)
        val U64 = Type(TypeKind.U64, 0)

        val I8  = Type(TypeKind.I8, 0)
        val I16 = Type(TypeKind.I16, 0)
        val I32 = Type(TypeKind.I32, 0)
        val I64 = Type(TypeKind.I64, 0)

        val Void  = Type(TypeKind.VOID, -1)
        val UNDEF = Type(TypeKind.UNDEFINED, -1)

        fun of(kind: TypeKind, indirections: Int): Type {
            return Type(kind, indirections)
        }
    }
}