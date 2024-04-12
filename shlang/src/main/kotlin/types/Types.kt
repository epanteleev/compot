package types


open class BaseType(open val name: String, val size: Int): TypeProperty {
    override fun toString(): String = name

    companion object {
        val VOID: BaseType = BaseType("void", 0)
        val BOOL = BaseType("bool", 1)
        val CHAR = BaseType("char", 1)
        val SHORT = BaseType("short", 2)
        val INT = BaseType("int", 4)
        val LONG = BaseType("long", 8)
        val FLOAT = BaseType("float", 4)
        val DOUBLE = BaseType("double", 8)
        val UCHAR = BaseType("unsigned char", 1)
        val USHORT = BaseType("unsigned short", 2)
        val UINT = BaseType("unsigned int", 4)
        val ULONG = BaseType("unsigned long", 8)
        val UNKNOWN = BaseType("<unknown>", 0)
    }
}

open class UncompletedType(override val name: String): BaseType(name, 0)

interface CType {
   fun baseType(): BaseType
   fun qualifiers(): List<TypeProperty>

    companion object {
        val UNRESOlVED: CType = NoType("<unresolved>")
        val UNKNOWN: CType = NoType("<unknown>")
        val INT = CPrimitiveType(BaseType.INT)
        val CHAR = CPrimitiveType(BaseType.CHAR)
        val VOID = CPrimitiveType(BaseType.INT)
        val FLOAT = CPrimitiveType(BaseType.FLOAT)
        val DOUBLE = CPrimitiveType(BaseType.DOUBLE)
        val LONG = CPrimitiveType(BaseType.LONG)
        val SHORT = CPrimitiveType(BaseType.SHORT)
        val UINT = CPrimitiveType(BaseType.UINT)
        val BOOL = CPrimitiveType(BaseType.BOOL)

        fun interfereTypes(type1: CType, type2: CType): CType {
            if (type1 == type2) return type1
            if (type1 == UNRESOlVED) return UNRESOlVED
            if (type2 == UNRESOlVED) return UNRESOlVED
            if (type1 == UNKNOWN) return type2
            if (type2 == UNKNOWN) return type1

            when {
                type1.baseType() == BaseType.DOUBLE && type2.baseType() == BaseType.FLOAT -> return type1
                type1.baseType() == BaseType.FLOAT && type2.baseType() == BaseType.DOUBLE -> return type2
                type1.baseType() == BaseType.LONG && type2.baseType() == BaseType.INT -> return type1
                type1.baseType() == BaseType.INT && type2.baseType() == BaseType.LONG -> return type2
                type1.baseType() == BaseType.UINT && type2.baseType() == BaseType.INT -> return type1
                type1.baseType() == BaseType.INT && type2.baseType() == BaseType.UINT -> return type2
                type1.baseType() == BaseType.ULONG && type2.baseType() == BaseType.LONG -> return type1
                type1.baseType() == BaseType.LONG && type2.baseType() == BaseType.ULONG -> return type2
                type1.baseType() == BaseType.USHORT && type2.baseType() == BaseType.SHORT -> return type1
                type1.baseType() == BaseType.SHORT && type2.baseType() == BaseType.USHORT -> return type2
                type1.baseType() == BaseType.UCHAR && type2.baseType() == BaseType.CHAR -> return type1
                type1.baseType() == BaseType.CHAR && type2.baseType() == BaseType.UCHAR -> return type2
            }
            return UNRESOlVED
        }
    }
}

interface AnyCPointerType: CType

data class CPointerType(val type: CType) : AnyCPointerType {
    override fun baseType(): BaseType = BaseType.UNKNOWN
    override fun qualifiers(): List<TypeProperty> = emptyList()

    override fun toString(): String {
        return buildString {
            append(type)
            append("*")
        }
    }
}

data class CFunPointerType(val returnType: CType, val argsTypes: List<CType>) : AnyCPointerType {
    override fun baseType(): BaseType = BaseType.UNKNOWN
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
    override fun baseType(): BaseType = BaseType.UNKNOWN
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
            is StructDeclaration -> {
                val struct = CStructType(baseType.name, properties.filterNot { it is BaseType })
                typeHolder.addStructType(baseType.name, struct)
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

data class CStructType(val name: String, val qualifiers: List<TypeProperty>) : CType { //TODO
    val fields = mutableListOf<Pair<String, CType>>()

    fun addField(name: String, type: CType) {
        fields.add(name to type)
    }

    override fun toString(): String {
        return buildString {
            qualifiers.forEach { append("$it ") }
            append("struct $name")
            if (fields.isNotEmpty()) {
                append(" {\n")
                fields.forEach { (name, type) ->
                    append("    $type $name;\n")
                }
                append("}")
            }
        }
    }

    override fun baseType(): BaseType = BaseType.UNKNOWN
    override fun qualifiers(): List<TypeProperty> = emptyList()
}

class CUnionType() : CType { //TODO
    val fields = mutableListOf<Pair<String, CType>>()

    fun addField(name: String, type: CType) {
        fields.add(name to type)
    }

    override fun baseType(): BaseType = BaseType.UNKNOWN
    override fun qualifiers(): List<TypeProperty> = emptyList()
}

class CEnumType() : CType { //TODO
    val fields = mutableListOf<Pair<String, Int>>()

    fun addField(name: String, value: Int) {
        fields.add(name to value)
    }

    override fun baseType(): BaseType = BaseType.UNKNOWN
    override fun qualifiers(): List<TypeProperty> = emptyList()
}

data class StructDeclaration(override val name: String): UncompletedType(name) {
    override fun toString(): String = "struct $name"
}

data class CArrayType(val type: CType, val size: Int) : CType {
    override fun baseType(): BaseType = type.baseType()
    override fun qualifiers(): List<TypeProperty> = type.qualifiers()
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
