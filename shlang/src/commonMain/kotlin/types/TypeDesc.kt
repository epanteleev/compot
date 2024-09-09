package types

import common.assertion
import ir.Definitions.POINTER_SIZE


data class TypeInferenceException(override val message: String) : Exception(message)

data class TypeResolutionException(override val message: String) : Exception(message)

sealed class TypeDesc(val properties: List<TypeProperty>) {
    abstract fun qualifiers(): List<TypeProperty>
    fun storageClass(): StorageClass? {
        assertion(qualifiers().filterIsInstance<StorageClass>().size <= 1) {
            "Multiple storage classes in type $this"
        }
        return qualifiers().firstOrNull { it is StorageClass } as StorageClass?
    }

    abstract fun size(): Int
    abstract fun copyWith(extraProperties: List<TypeProperty>): TypeDesc

    companion object {
        val UNRESOlVED: TypeDesc = NoType("<unresolved>")
        val CINT = CPrimitiveType(INT)
        val CCHAR = CPrimitiveType(CHAR)
        val CVOID = CPrimitiveType(VOID)
        val CFLOAT = CPrimitiveType(FLOAT)
        val CDOUBLE = CPrimitiveType(DOUBLE)
        val CLONG = CPrimitiveType(LONG)
        val CSHORT = CPrimitiveType(SHORT)
        val CUINT = CPrimitiveType(UINT)
        val CUSHORT = CPrimitiveType(USHORT)
        val CUCHAR = CPrimitiveType(UCHAR)
        val CULONG = CPrimitiveType(ULONG)
        val CBOOL = CPrimitiveType(BOOL)

        fun interfereTypes(type1: TypeDesc, type2: TypeDesc): TypeDesc {
            if (type1 == type2) return type1
            if (type1 == UNRESOlVED) return UNRESOlVED
            if (type2 == UNRESOlVED) return UNRESOlVED

            when (type1) {
                CCHAR -> {
                    return when (type2) {
                        CINT -> CINT
                        CLONG -> CLONG
                        CULONG -> CULONG
                        CSHORT -> CSHORT
                        CUINT -> CUINT
                        CDOUBLE -> CDOUBLE
                        CFLOAT -> CFLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                CUCHAR -> {
                    return when (type2) {
                        CINT -> CINT
                        CLONG -> CLONG
                        CCHAR -> CUCHAR
                        CUINT -> CUINT
                        CDOUBLE -> CDOUBLE
                        CFLOAT -> CFLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }
                CSHORT -> {
                    return when (type2) {
                        CINT -> CINT
                        CLONG -> CLONG
                        CCHAR -> CSHORT
                        CUINT -> CUINT
                        CDOUBLE -> CDOUBLE
                        CFLOAT -> CFLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                CINT -> {
                    return when (type2) {
                        CCHAR -> CINT
                        CUCHAR -> CINT
                        CLONG -> CLONG
                        CULONG -> CULONG
                        CSHORT -> CINT
                        CUSHORT -> CINT
                        CUINT -> CUINT
                        CDOUBLE -> CDOUBLE
                        CFLOAT -> CFLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                CLONG -> {
                    return when (type2) {
                        CCHAR -> CLONG
                        CINT -> CLONG
                        CSHORT -> CLONG
                        CUINT -> CUINT
                        CULONG -> CULONG
                        CDOUBLE -> CDOUBLE
                        CFLOAT -> CFLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                CFLOAT -> {
                    return when (type2) {
                        CCHAR -> CFLOAT
                        CINT -> CFLOAT
                        CSHORT -> CFLOAT
                        CUINT -> CFLOAT
                        CDOUBLE -> CDOUBLE
                        CLONG -> CDOUBLE
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                CDOUBLE -> {
                    return when (type2) {
                        CCHAR -> CDOUBLE
                        CINT -> CDOUBLE
                        CSHORT -> CDOUBLE
                        CUINT -> CDOUBLE
                        CFLOAT -> CDOUBLE
                        CLONG -> CDOUBLE
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                CUSHORT -> {
                    return when (type2) {
                        CINT -> CINT
                        CLONG -> CLONG
                        CCHAR -> CUSHORT
                        CUINT -> CUINT
                        CDOUBLE -> CDOUBLE
                        CFLOAT -> CFLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                CUINT -> {
                    when (type2) {
                        CCHAR -> return CUINT
                        CUCHAR -> return CUINT
                        CLONG -> return CLONG
                        CULONG -> return CULONG
                        CSHORT -> return CUINT
                        CUSHORT -> return CUINT
                        CINT -> return CUINT
                        CDOUBLE -> return CDOUBLE
                        CFLOAT -> return CFLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                CULONG -> {
                    return when (type2) {
                        CCHAR -> CULONG
                        CUCHAR -> CULONG
                        CINT -> CULONG
                        CLONG -> CULONG
                        CSHORT -> CULONG
                        CUSHORT -> CULONG
                        CUINT -> CULONG
                        CDOUBLE -> CDOUBLE
                        CFLOAT -> CFLOAT
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                is CPointerType -> {
                    when (type2) {
                        CCHAR -> return type1
                        CINT -> return type1
                        CSHORT -> return type1
                        CUINT -> return type1
                        CFLOAT -> return type1
                        CLONG -> return type1
                        is CPointerType -> {
                            if (type1.type == type2.type) return type1
                            if (type1.type == CVOID) return type1
                            if (type2.type == CVOID) return type2
                        }
                        CULONG -> return type1
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                else -> throw RuntimeException("Unknown type $type1, $type2")
            }
            throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
        }
    }
}

sealed class AnyCPointerType(properties: List<TypeProperty>): TypeDesc(properties) {
    override fun size(): Int = POINTER_SIZE

    abstract fun dereference(): TypeDesc
}

class CPointerType(val type: TypeDesc, properties: List<TypeProperty> = listOf()) : AnyCPointerType(properties) {
    override fun qualifiers(): List<TypeProperty> = properties

    override fun dereference(): TypeDesc = type

    override fun toString(): String = buildString {
        append(type)
        append("*")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CPointerType) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun copyWith(extraProperties: List<TypeProperty>): CPointerType {
        return CPointerType(type, qualifiers() + extraProperties)
    }
}

class CFunPointerType(private val cFunctionType: AbstractCFunctionType, properties: List<TypeProperty> = emptyList()) : AnyCPointerType(properties) {
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

    fun retType() = cFunctionType.retType
    fun args() = cFunctionType.argsTypes
    fun isVariadic() = cFunctionType.variadic

    override fun dereference(): TypeDesc = cFunctionType

    override fun copyWith(extraProperties: List<TypeProperty>): CFunPointerType {
        return CFunPointerType(cFunctionType.copyWith(extraProperties))
    }
}

data class NoType(val message: String) : TypeDesc(listOf()) {
    override fun size(): Int = UNKNOWN.size()
    override fun qualifiers(): List<TypeProperty> = emptyList()
    override fun copyWith(extraProperties: List<TypeProperty>): TypeDesc {
        TODO("Not yet implemented")
    }
}

class CPrimitiveType(val baseType: BaseType, properties: List<TypeProperty> = arrayListOf()) : TypeDesc(properties) {
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

        return baseType == other.baseType
    }

    override fun hashCode(): Int {
        return baseType.hashCode()
    }

    override fun copyWith(extraProperties: List<TypeProperty>): CPrimitiveType {
        return CPrimitiveType(baseType, properties + extraProperties)
    }
}

data class AbstractCFunctionType(val retType: TypeDesc, val argsTypes: List<TypeDesc>, var variadic: Boolean): TypeDesc(listOf()) {
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

class CFunctionType(val name: String, val functionType: AbstractCFunctionType, properties: List<TypeProperty> = arrayListOf()) : TypeDesc(properties) {
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

sealed class CompoundType(properties: List<TypeProperty>) : TypeDesc(properties) {
    override fun qualifiers(): List<TypeProperty> = properties
}

sealed class CommonCArrayType(properties: List<TypeProperty>): CompoundType(properties) {
    abstract fun element(): TypeDesc
}


class CArrayType(private val elementType: CArrayBaseType, properties: List<TypeProperty> = emptyList()) : CommonCArrayType(properties) {
    override fun size(): Int {
        return elementType.size()
    }

    override fun element(): TypeDesc = elementType.type

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

    override fun copyWith(extraProperties: List<TypeProperty>): TypeDesc {
        return CArrayType(elementType, properties + extraProperties)
    }
}

class UncompletedArrayType(private val elementType: TypeDesc, properties: List<TypeProperty> = emptyList()) : CommonCArrayType(properties) {
    override fun size(): Int {
        return POINTER_SIZE
    }

    override fun element(): TypeDesc = elementType

    override fun qualifiers(): List<TypeProperty> = properties

    override fun toString(): String {
        return buildString {
            properties.forEach { append("$it ") }
            append("[]")
            append(elementType)
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

    override fun copyWith(extraProperties: List<TypeProperty>): TypeDesc {
        return UncompletedArrayType(elementType, properties + extraProperties)
    }
}

sealed class CBaseStructType(protected open val baseType: AnyStructType, properties: List<TypeProperty> = emptyList()) : CompoundType(properties) {
    override fun toString(): String {
        return buildString {
            properties.forEach { append("$it ") }
            append(baseType)
        }
    }

    fun fieldIndex(name: String): Int {
        return baseType.fieldIndex(name)
    }

    fun fieldType(idx: Int): TypeDesc {
        return baseType.fieldIndex(idx)
    }

    fun fields(): List<Pair<String, TypeDesc>> {
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

    fun fieldIndex(idx: Int): TypeDesc {
        return baseType.fieldIndex(idx)
    }
}

class CUnionType(baseType: UnionBaseType, properties: List<TypeProperty> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeProperty>): TypeDesc {
        return CUnionType(baseType as UnionBaseType, properties + extraProperties)
    }
}

class CUncompletedStructType(baseType: UncompletedStructBaseType, properties: List<TypeProperty> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeProperty>): TypeDesc {
        return CUncompletedStructType(baseType as UncompletedStructBaseType, properties + extraProperties)
    }
}

class CUncompletedUnionType(baseType: UncompletedUnionBaseType, properties: List<TypeProperty> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeProperty>): TypeDesc {
        return CUncompletedUnionType(baseType as UncompletedUnionBaseType, properties + extraProperties)
    }
}

class CUncompletedEnumType(val baseType: UncompletedEnumType, properties: List<TypeProperty> = emptyList()) : TypeDesc(properties) {
    override fun copyWith(extraProperties: List<TypeProperty>): TypeDesc {
        return CUncompletedEnumType(baseType, properties + extraProperties)
    }

    override fun qualifiers(): List<TypeProperty> {
        return properties
    }

    override fun size(): Int {
        return baseType.size()
    }
}

class CEnumType(private val baseType: EnumBaseType, properties: List<TypeProperty> = emptyList()) : CompoundType(properties) {
    override fun size(): Int {
        return baseType.size()
    }

    override fun toString(): String {
        return buildString {
            properties.forEach { append("$it ") }
            append(baseType)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CEnumType) return false

        if (baseType != other.baseType) return false

        return true
    }

    override fun hashCode(): Int {
        return baseType.hashCode()
    }

    override fun copyWith(extraProperties: List<TypeProperty>): TypeDesc {
        return CEnumType(baseType, properties + extraProperties)
    }
}