package types

import ir.Definitions.BYTE_SIZE


sealed class CPrimitive: CompletedType() {
    final override fun alignmentOf(): Int = size()

    fun interfere(type2: CPrimitive): CPrimitive? {
        when (this) {
            type2 -> return this
            CHAR -> {
                return when (type2) {
                    BOOL -> CHAR
                    UCHAR -> UCHAR
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
                    SHORT -> SHORT
                    USHORT -> USHORT
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
                    UCHAR -> SHORT
                    USHORT -> USHORT
                    UINT -> UINT
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> INT
                    else -> null
                }
            }

            INT -> {
                return when (type2) {
                    BOOL, CHAR, UCHAR, SHORT, USHORT -> INT
                    LONG -> LONG
                    ULONG -> ULONG
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
                    BOOL, CHAR, UCHAR, INT, SHORT, USHORT, UINT -> LONG
                    ULONG -> ULONG
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> LONG
                    is CPointer -> LONG
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
                    LONG -> FLOAT
                    is CEnumType -> FLOAT
                    else -> null
                }
            }

            DOUBLE -> return DOUBLE
            USHORT -> {
                return when (type2) {
                    BOOL -> USHORT
                    INT -> INT
                    SHORT -> USHORT
                    LONG -> LONG
                    ULONG -> ULONG
                    CHAR -> USHORT
                    UCHAR -> USHORT
                    UINT -> UINT
                    DOUBLE -> DOUBLE
                    FLOAT -> FLOAT
                    is CEnumType -> INT
                    is CPointer -> type2
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
                    is CPointer -> type2
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
                    is CPointer -> type2
                    else -> null
                }
            }

            is CPointer -> return this
            is CEnumType -> {
                return when (type2) {
                    BOOL -> INT
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
                    is CPointer -> type2
                    is CEnumType -> {
                        if (this == type2) {
                            return this
                        }
                        return INT
                    }
                    else -> return null
                }
            }
            is BOOL -> return type2
            else -> return null
        }
    }
}

sealed class AnyCInteger: CPrimitive()

data object VOID: CPrimitive() {
    override fun toString(): String = "void"
    override fun size(): Int = BYTE_SIZE
}

data object BOOL: CPrimitive() {
    override fun toString(): String = "_Bool"
    override fun size(): Int = BYTE_SIZE
}