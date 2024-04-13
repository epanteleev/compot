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

            when {
                type1.baseType() == CPrimitive.INT && type2.baseType() == CPrimitive.INT -> return INT
                type1.baseType() == CPrimitive.DOUBLE && type2.baseType() == CPrimitive.DOUBLE -> return DOUBLE
                type1.baseType() == CPrimitive.FLOAT && type2.baseType() == CPrimitive.FLOAT -> return FLOAT
                type1.baseType() == CPrimitive.LONG && type2.baseType() == CPrimitive.LONG -> return LONG
                type1.baseType() == CPrimitive.SHORT && type2.baseType() == CPrimitive.SHORT -> return SHORT
                type1.baseType() == CPrimitive.UINT && type2.baseType() == CPrimitive.UINT -> return UINT
                type1.baseType() == CPrimitive.CHAR && type2.baseType() == CPrimitive.CHAR -> return CHAR

            }
            return UNRESOlVED
        }
    }
}

interface AnyCPointerType: CType

data class CPointerType(val type: CType) : AnyCPointerType {
    override fun baseType(): BaseType = CPrimitive.UNKNOWN
    override fun qualifiers(): List<TypeProperty> = emptyList()

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
