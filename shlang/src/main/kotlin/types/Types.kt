package types

data class BaseType private constructor(val name: String, val size: Int) {
    override fun toString(): String = name

    companion object {
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
    }
}


interface CType {
   fun baseType(): BaseType
   fun qualifiers(): List<TypeProperty>

   companion object {
       val INT = CPrimitiveType(BaseType.INT)
       val CHAR = CPrimitiveType(BaseType.CHAR)
       val VOID = CPrimitiveType(BaseType.INT)
       val FLOAT = CPrimitiveType(BaseType.FLOAT)
       val DOUBLE = CPrimitiveType(BaseType.DOUBLE)
       val LONG = CPrimitiveType(BaseType.LONG)
       val SHORT = CPrimitiveType(BaseType.SHORT)
       val UINT = CPrimitiveType(BaseType.UINT)
       val BOOL = CPrimitiveType(BaseType.BOOL)
   }
}

class CTypeBuilder {
    var basicType: BaseType? = null
    private val properties = mutableListOf<TypeProperty>()

    fun basicType(type: BaseType) {
        basicType = type
    }

    fun add(property: TypeProperty) {
        properties.add(property)
    }

    fun addAll(properties: List<TypeProperty>) {
        this.properties.addAll(properties)
    }

    fun build(): CType {
        return CPrimitiveType(basicType as BaseType, properties)
    }
}

data class CPrimitiveType(val baseType: BaseType, val properties: List<TypeProperty> = emptyList()) : CType {
    override fun baseType(): BaseType = baseType
    override fun qualifiers(): List<TypeProperty> = properties
}

data class FunctionType(val name: String, val retType: CType, val args: List<String>, val argsTypes: List<CType>, var variadic: Boolean = false) : CType {
    override fun baseType(): BaseType = retType.baseType()
    override fun qualifiers(): List<TypeProperty> = retType.qualifiers()
}