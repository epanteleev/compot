package types

class CTypeBuilder {
    private val properties = mutableListOf<TypeProperty>()

    fun add(property: TypeProperty) {
        properties.add(property)
    }

    private fun check(baseTypes: List<BaseType>, vararg types: CPrimitive): Boolean {
        if (baseTypes.size != types.size) {
            return false
        }
        types.forEachIndexed { index, type ->
            if (baseTypes[index] != type) {
                return false
            }
        }
        return true
    }

    private fun finalBaseType(baseTypes: List<BaseType>): BaseType {
        when {
            check(baseTypes, CPrimitive.UINT, CPrimitive.CHAR) -> return CPrimitive.UCHAR
        }
        return baseTypes[0]
    }

    fun build(typeHolder: TypeHolder): CType {
        val typeNodes = properties.filterIsInstance<BaseType>()
        val baseType = if (typeNodes[0] is TypeDef) {
            return (typeNodes[0] as TypeDef).baseType().copyWith(properties.filterNot { it is BaseType })
        } else {
            typeNodes[0]
        }

        if (baseType !is AggregateBaseType) {
            return CPrimitiveType(baseType, properties.filterNot { it is BaseType })
        }
        val properties = properties.filterNot { it is BaseType }
        val struct = when (baseType) {
            is StructBaseType            -> CStructType(baseType, properties)
            is UnionBaseType             -> CUnionType(baseType, properties)
            is UncompletedStructBaseType -> CUncompletedStructType(baseType, properties)
            is UncompletedUnionBaseType  -> CUncompletedUnionType(baseType, properties)
            is CArrayBaseType            -> CArrayType(baseType, properties)
            else -> throw RuntimeException("Unknown type $baseType")
        }

        return when (baseType) {
            is StructBaseType -> {
                typeHolder.addTypedef(baseType.name, struct)
            }
            is UnionBaseType -> {
                typeHolder.addTypedef(baseType.name, struct)
            }
            else -> struct
        }
    }
}