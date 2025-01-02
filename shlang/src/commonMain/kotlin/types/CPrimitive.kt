package types

import typedesc.*
import ir.Definitions.BYTE_SIZE
import ir.Definitions.DOUBLE_SIZE
import ir.Definitions.FLOAT_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.POINTER_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE


sealed class CPrimitive: CType() {
    final override fun alignmentOf(): Int = size()

    fun interfere(typeHolder: TypeHolder, type2: CType): CPrimitive? {
        when (this) {
            type2 -> return this
            CHAR -> {
                return when (type2) {
                    BOOL -> CHAR
                    INT -> INT
                    LONG -> LONG
                    ULONG -> ULONG
                    SHORT -> SHORT
                    UINT -> UINT
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> INT
                    else -> null
                }
            }

            UCHAR -> {
                return when (type2) {
                    BOOL -> UCHAR
                    INT -> INT
                    LONG -> LONG
                    CHAR -> UCHAR
                    UINT -> UINT
                    ULONG -> ULONG
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> INT
                    else -> null
                }
            }
            SHORT -> {
                return when (type2) {
                    BOOL -> SHORT
                    INT -> INT
                    LONG -> LONG
                    CHAR -> SHORT
                    UINT -> UINT
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> INT
                    else -> null
                }
            }

            INT -> {
                return when (type2) {
                    BOOL -> INT
                    CHAR -> INT
                    UCHAR -> INT
                    LONG -> LONG
                    ULONG -> ULONG
                    SHORT -> INT
                    USHORT -> INT
                    UINT -> UINT
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> INT
                    is CPointer -> type2
                    else -> null
                }
            }

            LONG -> {
                return when (type2) {
                    BOOL -> LONG
                    CHAR -> LONG
                    UCHAR -> LONG
                    INT -> LONG
                    SHORT -> LONG
                    USHORT -> LONG
                    UINT -> LONG
                    ULONG -> ULONG
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> LONG
                    else -> null
                }
            }

            FLOAT -> {
                return when (type2) {
                    BOOL -> FLOAT
                    CHAR -> FLOAT
                    INT -> FLOAT
                    SHORT -> FLOAT
                    UINT -> FLOAT
                    DOUBLE -> DOUBLE
                    LONG -> DOUBLE
                    is CEnumType -> FLOAT
                    else -> null
                }
            }

            DOUBLE -> {
                return when (type2) {
                    BOOL -> DOUBLE
                    CHAR -> DOUBLE
                    INT -> DOUBLE
                    SHORT -> DOUBLE
                    UINT -> DOUBLE
                    FLOAT -> DOUBLE
                    LONG -> DOUBLE
                    is CEnumType -> DOUBLE
                    else -> null
                }
            }

            USHORT -> {
                return when (type2) {
                    BOOL -> USHORT
                    INT -> INT
                    SHORT -> USHORT
                    LONG -> LONG
                    ULONG -> ULONG
                    CHAR -> USHORT
                    UINT -> UINT
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> INT
                    else -> null
                }
            }

            UINT -> {
                return when (type2) {
                    BOOL -> UINT
                    CHAR -> UINT
                    UCHAR -> UINT
                    LONG -> LONG
                    ULONG -> ULONG
                    SHORT -> UINT
                    USHORT -> UINT
                    INT -> UINT
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> UINT
                    else -> null
                }
            }

            ULONG -> {
                return when (type2) {
                    BOOL -> ULONG
                    CHAR -> ULONG
                    UCHAR -> ULONG
                    INT -> ULONG
                    LONG -> ULONG
                    SHORT -> ULONG
                    USHORT -> ULONG
                    UINT -> ULONG
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> ULONG
                    else -> null
                }
            }

            is CPointer -> {
                when (type2) {
                    CHAR -> return this
                    UCHAR -> return this
                    INT -> return this
                    SHORT -> return this
                    UINT -> return this
                    FLOAT -> return this
                    LONG -> return this
                    is CPointer -> {
                        val deref1 = dereference(typeHolder)
                        val deref2 = type2.dereference(typeHolder)
                        if (deref1 == deref2) return this
                        if (deref1 == VOID) return this
                        if (deref2 == VOID) return type2
                        return null
                    }
                    ULONG -> return this
                    else -> return null
                }
            }
            is CEnumType -> {
                return when (type2) {
                    CHAR -> INT
                    UCHAR -> INT
                    SHORT -> INT
                    USHORT -> INT
                    INT -> INT
                    UINT -> UINT
                    LONG -> LONG
                    ULONG -> ULONG
                    FLOAT -> FLOAT
                    DOUBLE -> DOUBLE
                    else -> return null
                }
            }
            is BOOL -> {
                return when (type2) {
                    CHAR -> CHAR
                    UCHAR -> UCHAR
                    SHORT -> SHORT
                    USHORT -> USHORT
                    INT -> INT
                    UINT -> UINT
                    LONG -> LONG
                    ULONG -> ULONG
                    FLOAT -> FLOAT
                    DOUBLE -> DOUBLE
                    else -> return null
                }
            }
            else -> return null
        }
    }
}

sealed class AnyCInteger: CPrimitive()

sealed class AnyCSigned: AnyCInteger()

data object VOID: CPrimitive() {
    override fun toString(): String = "void"
    override fun size(): Int = BYTE_SIZE
}

data object CHAR: AnyCSigned() {
    override fun toString(): String = "char"
    override fun size(): Int = BYTE_SIZE
}

data object SHORT: AnyCSigned() {
    override fun toString(): String = "short"
    override fun size(): Int = HWORD_SIZE
}

data object INT: AnyCSigned() {
    override fun toString(): String = "int"
    override fun size(): Int = WORD_SIZE
}

data object LONG: AnyCSigned() {
    override fun toString(): String = "long"
    override fun size(): Int = QWORD_SIZE
}

sealed class AnyCFloat: CPrimitive()

data object FLOAT: AnyCFloat() {
    override fun toString(): String = "float"
    override fun size(): Int = FLOAT_SIZE
}

data object DOUBLE: AnyCFloat() {
    override fun toString(): String = "double"
    override fun size(): Int = DOUBLE_SIZE
}

sealed class AnyCUnsigned: AnyCInteger()

data object UCHAR: AnyCUnsigned() {
    override fun toString(): String = "unsigned char"
    override fun size(): Int = BYTE_SIZE
}

data object USHORT: AnyCUnsigned() {
    override fun toString(): String = "unsigned short"
    override fun size(): Int = HWORD_SIZE
}

data object UINT: AnyCUnsigned() {
    override fun toString(): String = "unsigned int"
    override fun size(): Int = WORD_SIZE
}

data object ULONG: AnyCUnsigned() {
    override fun toString(): String = "unsigned long"
    override fun size(): Int = QWORD_SIZE
}

data object BOOL: CPrimitive() {
    override fun toString(): String = "_Bool"
    override fun size(): Int = BYTE_SIZE
}

class CPointer(val type: CType, private val properties: Set<TypeQualifier> = setOf()) : CPrimitive() {
    override fun size(): Int = POINTER_SIZE

    fun dereference(typeHolder: TypeHolder): CType= when (type) {
        is CFunctionType          -> type.functionType
        is CUncompletedStructType -> typeHolder.getStructType<CStructType>(type.name)
        is CUncompletedUnionType  -> typeHolder.getStructType<CUnionType>(type.name)
        is CUncompletedEnumType   -> typeHolder.getStructType<CEnumType>(type.name)
        else -> type
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CPointer) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String = buildString {
        properties.forEach { append(it) }
        append(type)
        append("*")
    }
}

data class CEnumType(val name: String, private val enumerators: Map<String, Int>): CPrimitive() {
    override fun toString(): String = name

    override fun size(): Int = INT.size()

    fun hasEnumerator(name: String): Boolean {
        return enumerators.contains(name)
    }

    fun enumerator(name: String): Int? {
        return enumerators[name] //TODO temporal
    }
}