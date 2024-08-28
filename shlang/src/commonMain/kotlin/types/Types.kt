package types

import ir.Definitions.POINTER_SIZE


data class TypeInferenceException(override val message: String) : Exception(message)

data class TypeResolutionException(override val message: String) : Exception(message)

sealed interface CType {
    fun qualifiers(): List<TypeProperty>
    fun size(): Int
    fun copyWith(extraProperties: List<TypeProperty>): CType

    companion object {
        val UNRESOlVED: CType = NoType("<unresolved>")
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
        val BOOL = CPrimitiveType(CPrimitive.BOOL)

        fun interfereTypes(type1: CType, type2: CType): CType {
            if (type1 == type2) return type1
            if (type1 == UNRESOlVED) return UNRESOlVED
            if (type2 == UNRESOlVED) return UNRESOlVED

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
                        SHORT -> INT
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
                        UINT -> UINT
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
                    when (type2) {
                        CHAR -> return UINT
                        UCHAR -> return UINT
                        LONG -> return LONG
                        ULONG -> return ULONG
                        SHORT -> return UINT
                        USHORT -> return UINT
                        INT -> return UINT
                        DOUBLE -> return DOUBLE
                        FLOAT -> return FLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                ULONG -> {
                    return when (type2) {
                        CHAR -> ULONG
                        INT -> ULONG
                        LONG -> ULONG
                        SHORT -> ULONG
                        UINT -> ULONG
                        DOUBLE -> DOUBLE
                        FLOAT -> FLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
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
                        is CPointerType -> {
                            if (type1.type == type2.type) return type1
                            if (type1.type == VOID) return type1
                            if (type2.type == VOID) return type2
                        }
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                else -> throw RuntimeException("Unknown type $type1, $type2")
            }
            throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
        }
    }
}

sealed interface AnyCPointerType: CType {
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

class CPrimitiveType(val baseType: BaseType, private val properties: List<TypeProperty> = emptyList()) : CType {
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
        return baseType.hashCode()
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

sealed class CompoundType(protected open val properties: List<TypeProperty>) : CType { //TODO
    override fun qualifiers(): List<TypeProperty> = properties
}

sealed class CommonCArrayType(properties: List<TypeProperty>): CompoundType(properties) {
    abstract fun element(): CType

    abstract fun hasUncompleted(): Boolean
}


class CArrayType(private val elementType: CArrayBaseType, override val properties: List<TypeProperty> = emptyList()) : CommonCArrayType(properties) {
    override fun size(): Int {
        return elementType.size()
    }

    override fun hasUncompleted(): Boolean {
        var currentType: CType = elementType.type
        while (currentType is CommonCArrayType) {
            if (currentType is UncompletedArrayType) {
                return true
            }
            currentType = currentType.element()
        }

        return false
    }

    override fun element(): CType = elementType.type

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

class UncompletedArrayType(private val elementType: CType, override val properties: List<TypeProperty> = emptyList()) : CommonCArrayType(properties) {
    override fun size(): Int {
        return POINTER_SIZE
    }

    override fun hasUncompleted(): Boolean = true

    override fun element(): CType = elementType

    override fun qualifiers(): List<TypeProperty> = properties

    override fun toString(): String {
        return buildString {
            properties.forEach { append("$it ") }
            append(elementType)
            append("[]")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UncompletedArrayType) return false

        if (elementType != other.elementType) return false

        return true
    }

    override fun hashCode(): Int {
        return elementType.hashCode()
    }

    override fun copyWith(extraProperties: List<TypeProperty>): CType {
        return UncompletedArrayType(elementType, properties + extraProperties)
    }
}

sealed class CBaseStructType(protected open val baseType: AnyStructType, override val properties: List<TypeProperty> = emptyList()) : CompoundType(properties) {
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