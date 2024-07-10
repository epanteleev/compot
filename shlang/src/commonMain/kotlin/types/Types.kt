package types

import ir.platform.x64.CallConvention.POINTER_SIZE


data class TypeInferenceException(override val message: String) : Exception(message)

interface CType {
    fun qualifiers(): List<TypeProperty>
    fun size(): Int
    fun copyWith(extraProperties: List<TypeProperty>): CType

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

                UCHAR -> {
                    when (type2) {
                        INT -> return INT
                        LONG -> return LONG
                        CHAR -> return UCHAR
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
                        UCHAR -> return INT
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

                USHORT -> {
                    when (type2) {
                        INT -> return INT
                        LONG -> return LONG
                        CHAR -> return USHORT
                        UINT -> return UINT
                        DOUBLE -> return DOUBLE
                        FLOAT -> return FLOAT
                    }
                }

                UINT -> {
                    when (type2) {
                        CHAR -> return UINT
                        LONG -> return LONG
                        SHORT -> return UINT
                        INT -> return UINT
                        DOUBLE -> return DOUBLE
                        FLOAT -> return FLOAT
                    }
                }

                ULONG -> {
                    when (type2) {
                        CHAR -> return ULONG
                        INT -> return ULONG
                        SHORT -> return ULONG
                        UINT -> return ULONG
                        DOUBLE -> return DOUBLE
                        FLOAT -> return FLOAT
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

interface AnyCPointerType: CType {
    override fun size(): Int = POINTER_SIZE //TODO must be imported from x64 module
}

data class CPointerType(val type: CType, val properties: List<TypeProperty> = listOf()) : AnyCPointerType {
    override fun qualifiers(): List<TypeProperty> = properties

    fun dereference(): CType = type

    override fun toString(): String {
        return buildString {
            append(type)
            append("*")
        }
    }

    override fun copyWith(extraProperties: List<TypeProperty>): CPointerType {
        return CPointerType(type, properties + extraProperties)
    }
}

data class CFunPointerType(val cFunctionType: AbstractCFunctionType) : AnyCPointerType {
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

    override fun copyWith(extraProperties: List<TypeProperty>): CFunPointerType {
        return CFunPointerType(cFunctionType.copyWith(extraProperties))
    }
}

data class NoType(val message: String) : CType {
    override fun size(): Int = CPrimitive.UNKNOWN.size()
    override fun qualifiers(): List<TypeProperty> = emptyList()
    override fun copyWith(extraProperties: List<TypeProperty>): CType {
        TODO("Not yet implemented")
    }
}

class CPrimitiveType(val baseType: BaseType, val properties: List<TypeProperty> = emptyList()) : CType {
    override fun size(): Int = baseType.size()
    override fun qualifiers(): List<TypeProperty> = properties

    override fun toString(): String {
        return buildString {
            properties.forEach { append("$it ") }
            append(baseType)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CPrimitiveType) return false

        if (baseType != other.baseType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = baseType.hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }

    override fun copyWith(extraProperties: List<TypeProperty>): CPrimitiveType {
        return CPrimitiveType(baseType, properties + extraProperties)
    }
}

data class AbstractCFunctionType(val retType: CType, val argsTypes: List<CType>, var variadic: Boolean): CType {
    override fun size(): Int = throw RuntimeException("Function type has no size")
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

    override fun copyWith(extraProperties: List<TypeProperty>): AbstractCFunctionType {
        return AbstractCFunctionType(retType.copyWith(extraProperties), argsTypes.map { it.copyWith(extraProperties) }, variadic)
    }
}

class CFunctionType(val name: String, val functionType: AbstractCFunctionType) : CType {
    override fun size(): Int = throw RuntimeException("Function type has no size")
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

    override fun copyWith(extraProperties: List<TypeProperty>): CFunctionType {
        return CFunctionType(name, functionType.copyWith(extraProperties))
    }
}

abstract class CompoundType(protected open val properties: List<TypeProperty>) : CType { //TODO
    override fun qualifiers(): List<TypeProperty> = properties
}

class CArrayType(val elementType: CArrayBaseType, override val properties: List<TypeProperty> = emptyList()) : CompoundType(properties) {
    override fun size(): Int {
        return elementType.size()
    }

    fun element(): CType = elementType.type

    fun dimension(): Long = elementType.dimension

    override fun qualifiers(): List<TypeProperty> = properties

    override fun toString(): String {
        return buildString {
            properties.forEach { append("$it ") }
            append(elementType)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CArrayType) return false

        if (elementType != other.elementType) return false

        return true
    }

    override fun hashCode(): Int {
        return elementType.hashCode()
    }

    override fun copyWith(extraProperties: List<TypeProperty>): CType {
        return CArrayType(elementType, properties + extraProperties)
    }
}

abstract class CBaseStructType(protected open val baseType: AnyStructType, override val properties: List<TypeProperty> = emptyList()) : CompoundType(properties) {
    override fun toString(): String {
        return buildString {
            properties.forEach { append("$it ") }
            append(baseType)
        }
    }

    fun fieldIndex(name: String): Int {
        return baseType.fieldIndex(name)
    }

    fun fields(): List<Pair<String, CType>> {
        return baseType.fields()
    }

    fun name(): String {
        return baseType.name
    }

    override fun size(): Int {
        return baseType.size()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CBaseStructType) return false

        if (baseType != other.baseType) return false

        return true
    }

    override fun hashCode(): Int {
        return baseType.hashCode()
    }
}

class CStructType(baseType: StructBaseType, properties: List<TypeProperty> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeProperty>): CStructType {
        return CStructType(baseType as StructBaseType, properties + extraProperties)
    }
}

class CUnionType(baseType: UnionBaseType, properties: List<TypeProperty> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeProperty>): CType {
        return CUnionType(baseType as UnionBaseType, properties + extraProperties)
    }
}

class CUncompletedStructType(baseType: UncompletedStructBaseType, properties: List<TypeProperty> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeProperty>): CType {
        return CUncompletedStructType(baseType as UncompletedStructBaseType, properties + extraProperties)
    }
}

class CUncompletedUnionType(baseType: UncompletedUnionBaseType, properties: List<TypeProperty> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeProperty>): CType {
        return CUncompletedUnionType(baseType as UncompletedUnionBaseType, properties + extraProperties)
    }
}