package types

import common.assertion

class CTypeBuilder {
    private val typeProperties = mutableListOf<TypeProperty>()

    fun add(property: TypeProperty) {
        typeProperties.add(property)
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
            check(baseTypes, UINT,  LONG,  LONG, INT) -> return ULONG
            check(baseTypes, UINT,  LONG,  LONG)      -> return LONG
            check(baseTypes, UINT,  SHORT, INT)       -> return USHORT
            check(baseTypes, UINT,  LONG,  INT)       -> return ULONG
            check(baseTypes, INT,   SHORT, INT)       -> return SHORT
            check(baseTypes, INT,   LONG,  INT)       -> return LONG
            check(baseTypes, LONG,  LONG,  INT)       -> return LONG
            check(baseTypes, LONG,  UINT,  INT)       -> return ULONG
            check(baseTypes, INT,   INT)              -> return INT
            check(baseTypes, UINT,  CHAR)             -> return UCHAR
            check(baseTypes, UINT,  SHORT)            -> return USHORT
            check(baseTypes, UINT,  INT)              -> return UINT
            check(baseTypes, UINT,  LONG)             -> return ULONG
            check(baseTypes, LONG,  LONG)             -> return LONG
            check(baseTypes, INT,   CHAR)             -> return CHAR
            check(baseTypes, LONG,  INT)              -> return LONG
            check(baseTypes, USHORT,INT)              -> return USHORT
            check(baseTypes, LONG,  DOUBLE)           -> return DOUBLE
        }
        assertion(baseTypes.size == 1) {
            "Unknown type '$baseTypes'"
        }
        return baseTypes[0]
    }

    fun build(typeHolder: TypeHolder, isStorageClassIncluded: Boolean): Pair<TypeDesc, StorageClass?> {
        val typeNodes = typeProperties.filterIsInstance<BaseType>()
        val storageClass = run {
            val classes = typeProperties.filterIsInstance<StorageClass>()
            if (classes.size > 1) {
                throw RuntimeException("Multiple storage classes are not allowed: $classes")
            }
            classes.firstOrNull()
        }
        val properties = typeProperties.filterIsInstance<TypeQualifier>()
        val baseType = if (typeNodes.size == 1 && typeNodes[0] is TypeDef) {
            val ctype = (typeNodes[0] as TypeDef).baseType().copyWith(properties)
            return Pair(ctype, storageClass)
        } else if (typeNodes[0] is CPrimitive) {
            finalBaseType(typeNodes)
        } else {
            typeNodes[0]
        }

        if (baseType !is AggregateBaseType) {
            val cType = CPrimitiveType(baseType, properties)
            return Pair(cType, storageClass)
        }

        val structType = when (baseType) {
            is StructBaseType            -> typeHolder.addTypedef(baseType.name, CStructType(baseType, properties))
            is UnionBaseType             -> typeHolder.addTypedef(baseType.name, CUnionType(baseType, properties))
            is UncompletedStructBaseType -> CUncompletedStructType(baseType, properties)
            is UncompletedUnionBaseType  -> CUncompletedUnionType(baseType, properties)
            is CArrayBaseType            -> CArrayType(baseType, properties)
            is EnumBaseType              -> CEnumType(baseType, properties)
            is UncompletedEnumType       -> CUncompletedEnumType(baseType, properties)
        }

        return Pair(structType, storageClass)
    }
}