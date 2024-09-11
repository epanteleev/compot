package types


data class TypeInferenceException(override val message: String) : Exception(message)

data class TypeResolutionException(override val message: String) : Exception(message)

class TypeDesc(val baseType: BaseType, val properties: List<TypeQualifier>) {
    fun qualifiers(): List<TypeQualifier> = properties
    fun baseType(): BaseType = baseType
    fun size(): Int = baseType.size()
    fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc {
        return TypeDesc(baseType, properties + extraProperties)
    }

    inline fun<reified T> asType(): T {
        return baseType() as T
    }

    override fun toString(): String = buildString {
        properties.forEach {
            append(it)
            append(" ")
        }
        append(baseType)
    }

    companion object {
        fun interfereTypes(type1: BaseType, type2: BaseType): BaseType {
            if (type1 == type2) return type1
            when (type1) {
                CHAR -> {
                    return when (type2) {
                        INT -> INT
                        LONG -> LONG
                        ULONG -> ULONG
                        SHORT -> SHORT
                        UINT -> UINT
                        DOUBLE -> DOUBLE
                        FLOAT -> FLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                UCHAR -> {
                    return when (type2) {
                        INT -> INT
                        LONG -> LONG
                        CHAR -> UCHAR
                        UINT -> UINT
                        DOUBLE -> DOUBLE
                        FLOAT -> FLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }
                SHORT -> {
                    return when (type2) {
                        INT -> INT
                        LONG -> LONG
                        CHAR -> SHORT
                        UINT -> UINT
                        DOUBLE -> DOUBLE
                        FLOAT -> FLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                INT -> {
                    return when (type2) {
                        CHAR -> INT
                        UCHAR -> INT
                        LONG -> LONG
                        ULONG -> ULONG
                        SHORT -> INT
                        USHORT -> INT
                        UINT -> UINT
                        DOUBLE -> DOUBLE
                        FLOAT -> FLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                LONG -> {
                    return when (type2) {
                        CHAR -> LONG
                        INT -> LONG
                        SHORT -> LONG
                        UINT -> LONG
                        ULONG -> ULONG
                        DOUBLE -> DOUBLE
                        FLOAT -> FLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                FLOAT -> {
                    return when (type2) {
                        CHAR -> FLOAT
                        INT -> FLOAT
                        SHORT -> FLOAT
                        UINT -> FLOAT
                        DOUBLE -> DOUBLE
                        LONG -> DOUBLE
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                DOUBLE -> {
                    return when (type2) {
                        CHAR -> DOUBLE
                        INT -> DOUBLE
                        SHORT -> DOUBLE
                        UINT -> DOUBLE
                        FLOAT -> DOUBLE
                        LONG -> DOUBLE
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                USHORT -> {
                    return when (type2) {
                        INT -> INT
                        LONG -> LONG
                        CHAR -> USHORT
                        UINT -> UINT
                        DOUBLE -> DOUBLE
                        FLOAT -> FLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                UINT -> {
                    return when (type2) {
                        CHAR -> UINT
                        UCHAR -> UINT
                        LONG -> LONG
                        ULONG -> ULONG
                        SHORT -> UINT
                        USHORT -> UINT
                        INT -> UINT
                        DOUBLE -> DOUBLE
                        FLOAT -> FLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                ULONG -> {
                    return when (type2) {
                        CHAR -> ULONG
                        UCHAR -> ULONG
                        INT -> ULONG
                        LONG -> ULONG
                        SHORT -> ULONG
                        USHORT -> ULONG
                        UINT -> ULONG
                        DOUBLE -> DOUBLE
                        FLOAT -> FLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                is CPointerT -> {
                    when (type2) {
                        CHAR -> return type1
                        INT -> return type1
                        SHORT -> return type1
                        UINT -> return type1
                        FLOAT -> return type1
                        LONG -> return type1
                        is CPointerT -> {
                            if (type1.type == type2.type) return type1
                            if (type1.dereference() == VOID) return type1
                            if (type2.dereference() == VOID) return type2
                        }
                        ULONG -> return type1
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                else -> throw RuntimeException("Unknown type $type1, $type2")
            }
            throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
        }

        fun from(baseType: BaseType, properties: List<TypeQualifier> = arrayListOf()): TypeDesc = TypeDesc(baseType, properties)
    }
}