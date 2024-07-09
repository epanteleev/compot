package types

import ir.platform.x64.CallConvention.POINTER_SIZE


data class TypeInferenceException(override val message: String) : Exception(message)

interface CType {
    fun baseType(): BaseType
    fun qualifiers(): List<TypeProperty>

    fun size(): Int {
        return baseType().size()
    }

    companion object {
        val UNRESOlVED: CType = NoType("<unresolved>")
        val UNKNOWN: CType = NoType("<unknown>")
        val INT = CPrimitiveType(CPrimitive.INT)
        val CHAR = CPrimitiveType(CPrimitive.CHAR)
        val VOID = CPrimitiveType(CPrimitive.VOID)
        val FLOAT = CPrimitiveType(CPrimitive.FLOAT)
        val DOUBLE = CPrimitiveType(CPrimitive.DOUBLE)
        val LONG = CPrimitiveType(CPrimitive.LONG)
        val SHORT = CPrimitiveType(CPrimitive.SHORT)
        val UINT = CPrimitiveType(CPrimitive.UINT)
        val USHORT = CPrimitiveType(CPrimitive.USHORT)
        val UCHAR = CPrimitiveType(CPrimitive.UCHAR)
        val ULONG = CPrimitiveType(CPrimitive.ULONG)

        fun interfereTypes(type1: CType, type2: CType): CType {
            if (type1 == type2) return type1
            if (type1 == UNRESOlVED) return UNRESOlVED
            if (type2 == UNRESOlVED) return UNRESOlVED
            if (type1 == UNKNOWN) return type2
            if (type2 == UNKNOWN) return type1
            if (type1.baseType() == type2.baseType()) {
                return type1
            }

            when (type1) {
                CHAR -> {
                    when (type2) {
                        INT -> return INT
                        LONG -> return LONG
                        SHORT -> return SHORT
                        UINT -> return UINT
                        DOUBLE -> return DOUBLE
                        FLOAT -> return FLOAT
                    }
                }

                SHORT -> {
                    when (type2) {
                        INT -> return INT
                        LONG -> return LONG
                        CHAR -> return SHORT
                        UINT -> return UINT
                        DOUBLE -> return DOUBLE
                        FLOAT -> return FLOAT
                    }
                }

                INT -> {
                    when (type2) {
                        CHAR -> return INT
                        LONG -> return LONG
                        SHORT -> return INT
                        UINT -> return UINT
                        DOUBLE -> return DOUBLE
                        FLOAT -> return FLOAT
                    }
                }

                LONG -> {
                    when (type2) {
                        CHAR -> return LONG
                        INT -> return LONG
                        SHORT -> return LONG
                        UINT -> return UINT
                        DOUBLE -> return DOUBLE
                        FLOAT -> return FLOAT
                    }
                }

                FLOAT -> {
                    when (type2) {
                        CHAR -> return FLOAT
                        INT -> return FLOAT
                        SHORT -> return FLOAT
                        UINT -> return FLOAT
                        DOUBLE -> return DOUBLE
                        LONG -> return DOUBLE
                    }
                }

                DOUBLE -> {
                    when (type2) {
                        CHAR -> return DOUBLE
                        INT -> return DOUBLE
                        SHORT -> return DOUBLE
                        UINT -> return DOUBLE
                        FLOAT -> return DOUBLE
                        LONG -> return DOUBLE
                    }
                }
                is CPointerType -> {
                    when (type2) {
                        CHAR -> return type1
                        INT -> return type1
                        SHORT -> return type1
                        UINT -> return type1
                        FLOAT -> return type1
                        LONG -> return type1
                    }
                }

                else -> throw RuntimeException("Unknown type $type1, $type2")
            }
            throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
        }
    }
}

data class TypedefType(val name: String, val type: CType) : CType {
    override fun baseType(): BaseType = type.baseType()
    override fun qualifiers(): List<TypeProperty>  {
        return listOf(StorageClass.TYPEDEF) + type.qualifiers()
    }
}

interface AnyCPointerType: CType

data class CPointerType(val type: CType) : AnyCPointerType {
    override fun size(): Int = POINTER_SIZE //TODO imported from x64 module
    override fun baseType(): BaseType = type.baseType()
    override fun qualifiers(): List<TypeProperty> = emptyList()

    fun dereference(): CType = type

    override fun toString(): String {
        return buildString {
            append(type)
            append("*")
        }
    }
}

data class CFunPointerType(val cFunctionType: AbstractCFunctionType) : AnyCPointerType {
    override fun baseType(): BaseType = CPrimitive.UNKNOWN
    override fun qualifiers(): List<TypeProperty> = emptyList()

    override fun toString(): String {
        return buildString {
            append(cFunctionType.retType)
            append("(*)(")
            cFunctionType.argsTypes.forEachIndexed { index, type ->
                append(type)
                if (index < cFunctionType.argsTypes.size - 1) append(", ")
            }
            append(")")
        }
    }
}

data class NoType(val message: String) : CType {
    override fun baseType(): BaseType = CPrimitive.UNKNOWN
    override fun qualifiers(): List<TypeProperty> = emptyList()
}

class CTypeBuilder {
    private val properties = mutableListOf<TypeProperty>()

    fun add(property: TypeProperty) {
        properties.add(property)
    }

    fun build(typeHolder: TypeHolder): CType {
        val typeNodes = properties.filterIsInstance<BaseType>()
        val baseType = typeNodes[0]
        if (baseType !is AggregateBaseType) {
            return CPrimitiveType(baseType, properties.filterNot { it is BaseType })
        }
        val struct = CompoundType(baseType, properties.filterNot { it is BaseType })
        when (baseType) {
            is StructBaseType, is UnionBaseType -> {
                typeHolder.addStructType(baseType.name, baseType)
                return struct
            }
            else -> {
                return struct
            }
        }
    }
}

data class CPrimitiveType(val baseType: BaseType, val properties: List<TypeProperty> = emptyList()) : CType {
    override fun baseType(): BaseType = baseType
    override fun qualifiers(): List<TypeProperty> = properties

    override fun toString(): String {
        return buildString {
            properties.forEach { append("$it ") }
            append(baseType)
        }
    }
}

data class AbstractCFunctionType(val retType: CType, val argsTypes: List<CType>, var variadic: Boolean): CType {
    override fun baseType(): BaseType = retType.baseType()
    override fun qualifiers(): List<TypeProperty> = retType.qualifiers()

    override fun toString(): String {
        return buildString {
            append(retType)
            append("(")
            argsTypes.forEachIndexed { index, type ->
                append(type)
                if (index < argsTypes.size - 1) append(", ")
            }
            if (variadic) append(", ...")
            append(")")
        }
    }
}

data class CFunctionType(val name: String, val functionType: AbstractCFunctionType) : CType {
    override fun baseType(): BaseType = functionType.baseType()
    override fun qualifiers(): List<TypeProperty> = functionType.qualifiers()

    fun retType() = functionType.retType
    fun args() = functionType.argsTypes

    override fun toString(): String {
        return buildString {
            append(functionType.retType)
            append(" $name(")
            functionType.argsTypes.forEachIndexed { index, type ->
                append(type)
                if (index < functionType.argsTypes.size - 1) append(", ")
            }
            if (functionType.variadic) append(", ...")
            append(")")
        }
    }
}

data class CompoundType(val baseType: AggregateBaseType, val properties: List<TypeProperty> = emptyList()) : CType { //TODO
    override fun toString(): String {
        return buildString {
            properties.forEach { append("$it ") }
            append(baseType)
        }
    }

    override fun baseType(): AggregateBaseType = baseType
    override fun qualifiers(): List<TypeProperty> = properties
}