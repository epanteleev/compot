package typedesc

import types.*
import codegen.VarStack


class TypeHolder private constructor(): Scope {
    private val valueMap = VarStack<VarDescriptor>()
    val enumTypeMap = VarStack<CType>()
    val structTypeMap = VarStack<CType>()
    val unionTypeMap = VarStack<CType>()

    private val functions = hashMapOf<String, VarDescriptor>()
    private val typedefs = VarStack<TypeDesc>()

    operator fun get(varName: String): VarDescriptor {
        return getVarTypeOrNull(varName) ?: throw Exception("Type for variable '$varName' not found")
    }

    fun getVarTypeOrNull(varName: String): VarDescriptor? {
        return valueMap[varName] ?: functions[varName]
    }

    fun findEnumByEnumerator(name: String): Int? {
        return findEnum(name)?.enumerator(name)
    }

    fun findEnum(name: String): CEnumType? {
        //TODO cache results
        for (enumType in enumTypeMap) {
            if (enumType !is CEnumType) {
                continue
            }
            if (enumType.hasEnumerator(name)) {
                return enumType
            }
        }
        return null
    }

    fun getUnionTypeOrNull(name: String): CType? = unionTypeMap[name]
    fun getStructTypeOrNull(name: String): CType? = structTypeMap[name]
    fun getEnumTypeOrNull(name: String): CType? = enumTypeMap[name]

    fun getStructType(name: String): CType {
        return getStructTypeOrNull(name) ?: throw Exception("Type for struct $name not found")
    }

    fun getEnumType(name: String): CType {
        return getEnumTypeOrNull(name) ?: throw Exception("Type for enum $name not found")
    }

    fun getUnionType(name: String): CType {
        return getUnionTypeOrNull(name) ?: throw Exception("Type for union $name not found")
    }

    fun getTypedefOrNull(name: String): TypeDesc? {
        return typedefs[name]
    }

    private fun addTypeDesc(name: String, structType: CType, qualifiers: List<TypeQualifier>): TypeDesc {
        val typeDesc = TypeDesc.from(structType, qualifiers)
        typedefs[name] = typeDesc
        return typeDesc
    }

    fun getTypedef(name: String): TypeDesc {
        val typeDesc = getTypedefOrNull(name) ?: throw Exception("Type for 'typedef $name' not found")
        when (val cType = typeDesc.cType()) {
            is CUncompletedStructType -> {
                val structType = getStructTypeOrNull(cType.name) ?: return typeDesc
                return addTypeDesc(name, structType, typeDesc.qualifiers())
            }
            is CUncompletedEnumType -> {
                val enumType = getEnumTypeOrNull(cType.name) ?: return typeDesc
                return addTypeDesc(name, enumType, typeDesc.qualifiers())
            }
            is CUncompletedUnionType -> {
                val unionType = getUnionTypeOrNull(cType.name) ?: return typeDesc
                return addTypeDesc(name, unionType, typeDesc.qualifiers())
            }
            else -> return typeDesc
        }
    }

    fun addTypedef(name: String, type: TypeDesc): TypeDesc {
        typedefs[name] = type
        return type
    }

    fun addVar(name: String, type: VarDescriptor): VarDescriptor {
        valueMap[name] = type
        return type
    }

    fun containsVar(varName: String): Boolean {
        return valueMap.containsKey(varName)
    }

    inline fun <reified T : CType> addNewType(name: String, type: T): T {
        when (type) {
            is CEnumType, is CUncompletedEnumType -> enumTypeMap[name] = type
            is CStructType, is CUncompletedStructType -> structTypeMap[name] = type
            is CUnionType, is CUncompletedUnionType -> unionTypeMap[name] = type
            else -> throw RuntimeException("Unknown type $type")
        }
        return type
    }

    fun getFunctionType(name: String): VarDescriptor {
        return functions[name] ?: valueMap[name] ?: throw Exception("Type for function '$name' not found")
    }

    fun addFunctionType(name: String, type: VarDescriptor): VarDescriptor {
        functions[name] = type
        return type
    }

    override fun enter() {
        enumTypeMap.enter()
        structTypeMap.enter()
        unionTypeMap.enter()
        valueMap.enter()
    }

    override fun leave() {
        valueMap.leave()
        unionTypeMap.leave()
        structTypeMap.leave()
        enumTypeMap.leave()
    }

    companion object {
        fun default(): TypeHolder {
            return TypeHolder()
        }
    }
}