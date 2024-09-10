package types

import ir.Definitions.POINTER_SIZE


data class TypeInferenceException(override val message: String) : Exception(message)

data class TypeResolutionException(override val message: String) : Exception(message)

sealed class TypeDesc(val properties: List<TypeQualifier>) {
    abstract fun qualifiers(): List<TypeQualifier>
    abstract fun baseType(): BaseType
    abstract fun size(): Int
    abstract fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc

    companion object {
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
                    return when (type2) {
                        CCHAR -> CUINT
                        CUCHAR -> CUINT
                        CLONG -> CLONG
                        CULONG -> CULONG
                        CSHORT -> CUINT
                        CUSHORT -> CUINT
                        CINT -> CUINT
                        CDOUBLE -> CDOUBLE
                        CFLOAT -> CFLOAT
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
                            if (type1.type.dereference() == CVOID) return type1
                            if (type2.type.dereference() == CVOID) return type2
                        }
                        CULONG -> return type1
                        else -> throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
                    }
                }

                else -> throw RuntimeException("Unknown type $type1, $type2")
            }
            throw TypeInferenceException("Can't interfere types '$type1' and '$type2'")
        }

        fun from(baseType: BaseType): TypeDesc = when (baseType) {
            is CPrimitive -> CPrimitiveType(baseType)
            is CPointerT -> CPointerType(baseType)
            is CArrayBaseType -> CArrayType(baseType)
            is CUncompletedArrayBaseType -> UncompletedArrayType(baseType)
            is AbstractCFunctionT -> AbstractCFunctionType(baseType, arrayListOf())
            is StructBaseType -> CStructType(baseType)
            is UnionBaseType -> CUnionType(baseType)
            is UncompletedStructBaseType -> CUncompletedStructType(baseType)
            is UncompletedUnionBaseType -> CUncompletedUnionType(baseType)
            is EnumBaseType -> CEnumType(baseType)
            is UncompletedEnumType -> CUncompletedEnumType(baseType)
            is TypeDef -> TODO()
        }
    }
}

sealed class AnyCPointerType(properties: List<TypeQualifier>): TypeDesc(properties) {
    override fun size(): Int = POINTER_SIZE

    abstract fun dereference(): TypeDesc
}

class CPointerType(val type: CPointerT, properties: List<TypeQualifier> = listOf()) : AnyCPointerType(properties) {
    override fun qualifiers(): List<TypeQualifier> = properties
    override fun baseType(): BaseType = type
    override fun dereference(): TypeDesc = type.dereference()

    override fun toString(): String = type.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CPointerType) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun copyWith(extraProperties: List<TypeQualifier>): CPointerType {
        return CPointerType(type, qualifiers() + extraProperties)
    }
}

class CFunPointerType(val type: CFunPointerT, properties: List<TypeQualifier> = emptyList()) : AnyCPointerType(properties) {
    override fun qualifiers(): List<TypeQualifier> = emptyList()
    override fun baseType(): BaseType = type
    override fun toString(): String = type.typename()

    fun retType() = type.functionType.retType
    fun args() = type.functionType.argsTypes
    fun isVariadic() = type.functionType.variadic

    override fun dereference(): TypeDesc = AbstractCFunctionType(type.functionType, arrayListOf())

    override fun copyWith(extraProperties: List<TypeQualifier>): CFunPointerType {
        return CFunPointerType(type, properties + extraProperties)
    }
}

class CPrimitiveType(val baseType: CPrimitive, properties: List<TypeQualifier> = arrayListOf()) : TypeDesc(properties) {
    override fun size(): Int = baseType.size()
    override fun qualifiers(): List<TypeQualifier> = properties
    override fun baseType(): BaseType = baseType

    override fun toString(): String = buildString {
        properties.forEach { append("$it ") }
        append(baseType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CPrimitiveType) return false

        return baseType == other.baseType
    }

    override fun hashCode(): Int {
        return baseType.hashCode()
    }

    override fun copyWith(extraProperties: List<TypeQualifier>): CPrimitiveType {
        return CPrimitiveType(baseType, properties + extraProperties)
    }
}

class AbstractCFunctionType(val baseType: AbstractCFunctionT, properties: List<TypeQualifier>): TypeDesc(properties) {
    override fun size(): Int = throw RuntimeException("Function type has no size")
    override fun qualifiers(): List<TypeQualifier> = properties
    override fun toString(): String = baseType.typename()
    override fun baseType(): BaseType = baseType

    override fun copyWith(extraProperties: List<TypeQualifier>): AbstractCFunctionType {
        return AbstractCFunctionType(baseType, properties + extraProperties)
    }
}

class CFunctionType(val name: String, val functionType: AbstractCFunctionT, properties: List<TypeQualifier> = arrayListOf()) : TypeDesc(properties) {
    override fun size(): Int = throw RuntimeException("Function type has no size")
    override fun qualifiers(): List<TypeQualifier> = properties
    override fun baseType(): BaseType = functionType
    fun retType() = functionType.retType
    fun args() = functionType.argsTypes

    override fun toString(): String = buildString {
        append(functionType.retType)
        append(" $name(")
        functionType.argsTypes.forEachIndexed { index, type ->
            append(type)
            if (index < functionType.argsTypes.size - 1) append(", ")
        }
        if (functionType.variadic) append(", ...")
        append(")")
    }

    override fun copyWith(extraProperties: List<TypeQualifier>): CFunctionType {
        return CFunctionType(name, functionType, properties + extraProperties)
    }
}

sealed class CompoundType(properties: List<TypeQualifier>) : TypeDesc(properties) {
    override fun qualifiers(): List<TypeQualifier> = properties
}

sealed class CommonCArrayType(properties: List<TypeQualifier>): CompoundType(properties) {
    abstract fun element(): TypeDesc
}

class CArrayType(private val elementType: CArrayBaseType, properties: List<TypeQualifier> = emptyList()) : CommonCArrayType(properties) {
    override fun size(): Int = elementType.size()
    override fun element(): TypeDesc = elementType.type
    fun dimension(): Long = elementType.dimension
    override fun baseType(): BaseType = elementType

    override fun qualifiers(): List<TypeQualifier> = properties

    override fun toString(): String = buildString {
        properties.forEach { append("$it ") }
        append(elementType)
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

    override fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc {
        return CArrayType(elementType, properties + extraProperties)
    }
}

class UncompletedArrayType(private val elementType: CUncompletedArrayBaseType, properties: List<TypeQualifier> = emptyList()) : CommonCArrayType(properties) {
    override fun size(): Int = POINTER_SIZE
    override fun element(): TypeDesc = elementType.elementType
    override fun qualifiers(): List<TypeQualifier> = properties
    override fun baseType(): BaseType = elementType

    override fun toString(): String = buildString {
        properties.forEach { append("$it ") }
        append(elementType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UncompletedArrayType) return false

        return elementType == other.elementType
    }

    override fun hashCode(): Int {
        return elementType.hashCode()
    }

    override fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc {
        return UncompletedArrayType(elementType, properties + extraProperties)
    }
}

sealed class CBaseStructType(protected open val baseType: AnyStructType, properties: List<TypeQualifier> = emptyList()) : CompoundType(properties) {
    override fun toString(): String = buildString {
        properties.forEach { append("$it ") }
        append(baseType)
    }

    override fun baseType(): BaseType = baseType

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

class CStructType(baseType: StructBaseType, properties: List<TypeQualifier> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeQualifier>): CStructType {
        return CStructType(baseType as StructBaseType, properties + extraProperties)
    }
}

class CUnionType(baseType: UnionBaseType, properties: List<TypeQualifier> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc {
        return CUnionType(baseType as UnionBaseType, properties + extraProperties)
    }
}

class CUncompletedStructType(baseType: UncompletedStructBaseType, properties: List<TypeQualifier> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc {
        return CUncompletedStructType(baseType as UncompletedStructBaseType, properties + extraProperties)
    }
}

class CUncompletedUnionType(baseType: UncompletedUnionBaseType, properties: List<TypeQualifier> = emptyList()) : CBaseStructType(baseType, properties) {
    override fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc {
        return CUncompletedUnionType(baseType as UncompletedUnionBaseType, properties + extraProperties)
    }
}

class CUncompletedEnumType(val baseType: UncompletedEnumType, properties: List<TypeQualifier> = emptyList()) : TypeDesc(properties) {
    override fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc {
        return CUncompletedEnumType(baseType, properties + extraProperties)
    }

    override fun baseType(): BaseType = baseType

    override fun qualifiers(): List<TypeQualifier> {
        return properties
    }

    override fun size(): Int {
        return baseType.size()
    }
}

class CEnumType(private val baseType: EnumBaseType, properties: List<TypeQualifier> = emptyList()) : CompoundType(properties) {
    override fun size(): Int {
        return baseType.size()
    }

    override fun baseType(): BaseType = baseType

    override fun toString(): String = buildString {
        properties.forEach { append("$it ") }
        append(baseType)
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

    override fun copyWith(extraProperties: List<TypeQualifier>): TypeDesc {
        return CEnumType(baseType, properties + extraProperties)
    }
}