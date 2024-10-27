package typedesc

import types.*
import codegen.VarStack


class TypeHolder private constructor(): Scope {
    private val valueMap = VarStack<VarDescriptor>()
    val enumTypeMap = VarStack<CPrimitive>()
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

    inline fun<reified T: CType> getTypeOrNull(name: String): CType? = when (T::class) {
        CEnumType::class, CUncompletedEnumType::class     -> enumTypeMap[name]
        CStructType::class, CUncompletedStructType::class -> structTypeMap[name]
        CUnionType::class, CUncompletedUnionType::class   -> unionTypeMap[name]
        else -> null
    }

    fun getTypedefOrNull(name: String): TypeDesc? {
        return typedefs[name]
    }

    fun getTypedef(name: String): TypeDesc {
        val t = getTypedefOrNull(name) ?: throw Exception("Type for 'typedef $name' not found")
        when (val cType = t.cType()) {
            is CUncompletedStructType -> {
                val structType = structTypeMap[cType.name] ?: return t
                val typeDesc = TypeDesc.from(structType, t.qualifiers())
                typedefs[name] = typeDesc
                return typeDesc
            }
            is CUncompletedEnumType -> {
                val enumType = enumTypeMap[cType.name] ?: return t
                val typeDesc = TypeDesc.from(enumType, t.qualifiers())
                typedefs[name] = typeDesc
                return typeDesc
            }
            is CUncompletedUnionType -> {
                val unionType = unionTypeMap[cType.name] ?: return t
                val typeDesc = TypeDesc.from(unionType, t.qualifiers())
                typedefs[name] = typeDesc
                return typeDesc
            }
            else -> return t
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

    inline fun<reified T: CType> getStructType(name: String): CType {
        return getTypeOrNull<T>(name) ?: throw Exception("Type for struct $name not found")
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