package types


interface CType {
   fun baseType(): BaseType
   fun qualifiers(): List<TypeProperty>

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

        fun interfereTypes(type1: CType, type2: CType): CType {
            if (type1 == type2) return type1
            if (type1 == UNRESOlVED) return UNRESOlVED
            if (type2 == UNRESOlVED) return UNRESOlVED
            if (type1 == UNKNOWN) return type2
            if (type2 == UNKNOWN) return type1

            when (type1.baseType()) {
                CPrimitive.CHAR -> {
                    when (type2.baseType()) {
                        CPrimitive.INT -> return INT
                        CPrimitive.LONG -> return LONG
                        CPrimitive.SHORT -> return SHORT
                        CPrimitive.UINT -> return UINT
                        CPrimitive.DOUBLE -> return DOUBLE
                        CPrimitive.FLOAT -> return FLOAT
                    }
                }
                CPrimitive.SHORT -> {
                    when (type2.baseType()) {
                        CPrimitive.INT -> return INT
                        CPrimitive.LONG -> return LONG
                        CPrimitive.CHAR -> return SHORT
                        CPrimitive.UINT -> return UINT
                        CPrimitive.DOUBLE -> return DOUBLE
                        CPrimitive.FLOAT -> return FLOAT
                    }
                }
                CPrimitive.INT -> {
                    when (type2.baseType()) {
                        CPrimitive.CHAR -> return INT
                        CPrimitive.LONG -> return LONG
                        CPrimitive.SHORT -> return INT
                        CPrimitive.UINT -> return UINT
                        CPrimitive.DOUBLE -> return DOUBLE
                        CPrimitive.FLOAT -> return FLOAT
                    }
                }
                CPrimitive.LONG -> {
                    when (type2.baseType()) {
                        CPrimitive.CHAR -> return LONG
                        CPrimitive.INT -> return LONG
                        CPrimitive.SHORT -> return LONG
                        CPrimitive.UINT -> return UINT
                        CPrimitive.DOUBLE -> return DOUBLE
                        CPrimitive.FLOAT -> return FLOAT
                    }
                }
                CPrimitive.FLOAT -> {
                    when (type2.baseType()) {
                        CPrimitive.CHAR -> return FLOAT
                        CPrimitive.INT -> return FLOAT
                        CPrimitive.SHORT -> return FLOAT
                        CPrimitive.UINT -> return FLOAT
                        CPrimitive.DOUBLE -> return DOUBLE
                        CPrimitive.LONG -> return DOUBLE
                    }
                }
                CPrimitive.DOUBLE -> {
                    when (type2.baseType()) {
                        CPrimitive.CHAR -> return DOUBLE
                        CPrimitive.INT -> return DOUBLE
                        CPrimitive.SHORT -> return DOUBLE
                        CPrimitive.UINT -> return DOUBLE
                        CPrimitive.FLOAT -> return DOUBLE
                        CPrimitive.LONG -> return DOUBLE
                    }
                }
                else -> throw RuntimeException("Unknown type $type1, $type2")
            }
            return UNRESOlVED
        }
    }
}

interface AnyCPointerType: CType

data class CPointerType(val type: CType) : AnyCPointerType {
    override fun baseType(): BaseType = CPrimitive.UNKNOWN
    override fun qualifiers(): List<TypeProperty> = emptyList()

    fun dereference(): CType = type

    override fun toString(): String {
        return buildString {
            append(type)
            append("*")
        }
    }
}

data class CFunPointerType(val returnType: CType, val argsTypes: List<CType>) : AnyCPointerType {
    override fun baseType(): BaseType = CPrimitive.UNKNOWN
    override fun qualifiers(): List<TypeProperty> = emptyList()

    override fun toString(): String {
        return buildString {
            append(returnType)
            append("(*)(")
            argsTypes.forEachIndexed { index, type ->
                append(type)
                if (index < argsTypes.size - 1) append(", ")
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
        when (baseType) {
            is StructBaseType -> {
                val struct = CompoundType(baseType, properties.filterNot { it is BaseType })
                typeHolder.addStructType(baseType.name, baseType)
                return struct
            }
        }
        return CPrimitiveType(baseType, properties.filterNot { it is BaseType })
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

data class CompoundType(val baseType: BaseType, val properties: List<TypeProperty> = emptyList()) : CType { //TODO
    val fields = mutableListOf<Pair<String, CType>>()

    override fun toString(): String {
        return baseType.toString()
    }

    override fun baseType(): BaseType = baseType
    override fun qualifiers(): List<TypeProperty> = properties
}

data class CFunctionType(val name: String, val retType: CType, val argsTypes: List<CType>, var variadic: Boolean = false) : CType {
    override fun baseType(): BaseType = retType.baseType()
    override fun qualifiers(): List<TypeProperty> = retType.qualifiers()

    override fun toString(): String {
        return buildString {
            append(retType)
            append(" $name(")
            argsTypes.forEachIndexed { index, type ->
                append(type)
                if (index < argsTypes.size - 1) append(", ")
            }
            if (variadic) append(", ...")
            append(")")
        }
    }
}
