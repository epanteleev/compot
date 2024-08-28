package types

import common.assertion

class CTypeBuilder {
    private val typeProperties = mutableListOf<TypeProperty>()

    fun add(property: TypeProperty) {
        typeProperties.add(property)
    }

    private inline fun check(baseTypes: List<BaseType>, vararg types: CPrimitive): Boolean {
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
            check(baseTypes, CPrimitive.UINT,  CPrimitive.LONG,  CPrimitive.LONG, CPrimitive.INT) -> return CPrimitive.ULONG
            check(baseTypes, CPrimitive.UINT,  CPrimitive.LONG,  CPrimitive.LONG) -> return CPrimitive.LONG
            check(baseTypes, CPrimitive.UINT,  CPrimitive.SHORT, CPrimitive.INT) -> return CPrimitive.USHORT
            check(baseTypes, CPrimitive.UINT,  CPrimitive.LONG,  CPrimitive.INT) -> return CPrimitive.ULONG
            check(baseTypes, CPrimitive.INT,   CPrimitive.SHORT, CPrimitive.INT) -> return CPrimitive.SHORT
            check(baseTypes, CPrimitive.INT,   CPrimitive.LONG,  CPrimitive.INT) -> return CPrimitive.LONG
            check(baseTypes, CPrimitive.LONG,  CPrimitive.LONG,  CPrimitive.INT) -> return CPrimitive.LONG
            check(baseTypes, CPrimitive.LONG,  CPrimitive.UINT,  CPrimitive.INT) -> return CPrimitive.ULONG
            check(baseTypes, CPrimitive.INT,   CPrimitive.INT)                   -> return CPrimitive.INT
            check(baseTypes, CPrimitive.UINT,  CPrimitive.CHAR)                  -> return CPrimitive.UCHAR
            check(baseTypes, CPrimitive.UINT,  CPrimitive.SHORT)                 -> return CPrimitive.USHORT
            check(baseTypes, CPrimitive.UINT,  CPrimitive.INT)                   -> return CPrimitive.UINT
            check(baseTypes, CPrimitive.UINT,  CPrimitive.LONG)                  -> return CPrimitive.ULONG
            check(baseTypes, CPrimitive.LONG,  CPrimitive.LONG)                  -> return CPrimitive.LONG
            check(baseTypes, CPrimitive.INT,   CPrimitive.CHAR)                  -> return CPrimitive.CHAR
            check(baseTypes, CPrimitive.LONG,  CPrimitive.INT)                   -> return CPrimitive.LONG
            check(baseTypes, CPrimitive.USHORT,CPrimitive.INT)                   -> return CPrimitive.USHORT
            check(baseTypes, CPrimitive.LONG,  CPrimitive.DOUBLE)                -> return CPrimitive.DOUBLE
        }
        assertion(baseTypes.size == 1) {
            "Unknown type '$baseTypes'"
        }
        return baseTypes[0]
    }

    fun build(typeHolder: TypeHolder, isStorageClassIncluded: Boolean): Pair<CType, StorageClass?> {
        val typeNodes = typeProperties.filterIsInstance<BaseType>()
        val storageClass = run {
            val classes = typeProperties.filterIsInstance<StorageClass>()
            if (classes.size > 1) {
                throw RuntimeException("Multiple storage classes are not allowed: $classes")
            }
            classes.firstOrNull()
        }
        val properties = typeProperties.filterNot {
            if (!isStorageClassIncluded) {
                return@filterNot it is StorageClass || it is BaseType
            }
            it is BaseType
        }
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

        val struct = when (baseType) {
            is StructBaseType            -> CStructType(baseType, properties)
            is UnionBaseType             -> CUnionType(baseType, properties)
            is UncompletedStructBaseType -> CUncompletedStructType(baseType, properties)
            is UncompletedUnionBaseType  -> CUncompletedUnionType(baseType, properties)
            is CArrayBaseType            -> CArrayType(baseType, properties)
            else -> throw RuntimeException("Unknown type $baseType")
        }

        val structType = when (baseType) {
            is StructBaseType -> {
                typeHolder.addTypedef(baseType.name, struct)
            }
            is UnionBaseType -> {
                typeHolder.addTypedef(baseType.name, struct)
            }
            else -> struct
        }

        return Pair(structType, storageClass)
    }
}