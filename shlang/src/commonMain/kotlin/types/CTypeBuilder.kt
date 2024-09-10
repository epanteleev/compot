package types

import common.assertion

class CTypeBuilder {
    private val typeProperties = mutableListOf<TypeQualifier>()
    private val baseTypes = mutableListOf<BaseType>()
    private var storageClass: StorageClass? = null

    fun add(property: TypeProperty) = when (property) {
        is BaseType -> baseTypes.add(property)
        is StorageClass -> {
            assertion(storageClass == null) {
                "Multiple storage classes are not allowed: $storageClass, $property"
            }
            storageClass = property
        }
        is TypeQualifier -> typeProperties.add(property)
        is FunctionSpecifier -> TODO()
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

    fun build(typeHolder: TypeHolder): VarDescriptor {
        val baseType = if (baseTypes.size == 1 && baseTypes[0] is TypeDef) {
            val ctype = (baseTypes[0] as TypeDef).baseType().copyWith(typeProperties)
            return VarDescriptor(ctype, storageClass)
        } else if (baseTypes[0] is CPrimitive) {
            finalBaseType(baseTypes)
        } else {
            baseTypes[0]
        }

        if (baseType is CPrimitive) {
            val cType = CPrimitiveType(baseType, typeProperties)
            return VarDescriptor(cType, storageClass)
        }

        val structType = when (baseType) {
            is StructBaseType            -> typeHolder.addTypedef(baseType.name, CStructType(baseType, typeProperties))
            is UnionBaseType             -> typeHolder.addTypedef(baseType.name, CUnionType(baseType, typeProperties))
            is UncompletedStructBaseType -> CUncompletedStructType(baseType, typeProperties)
            is UncompletedUnionBaseType  -> CUncompletedUnionType(baseType, typeProperties)
            is CArrayBaseType            -> CArrayType(baseType, typeProperties)
            is CUncompletedArrayBaseType      -> TODO()
            is EnumBaseType              -> CEnumType(baseType, typeProperties)
            is UncompletedEnumType       -> CUncompletedEnumType(baseType, typeProperties)
            else -> TODO()
        }

        return VarDescriptor(structType, storageClass)
    }
}