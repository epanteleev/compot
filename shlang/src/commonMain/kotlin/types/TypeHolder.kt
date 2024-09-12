package types

import gen.VarStack


class TypeHolder: Scope {
    private val valueMap = VarStack<VarDescriptor>()
    val enumTypeMap = VarStack<CPrimitive>()//TODO separate holder for struct, enum, union.
    val structTypeMap = VarStack<AggregateBaseType>()
    val unionTypeMap = VarStack<AggregateBaseType>()

    private val functions = hashMapOf<String, VarDescriptor>()
    private val typedefs = VarStack<TypeDesc>()

    operator fun get(varName: String): VarDescriptor {
        return getVarTypeOrNull(varName) ?: throw Exception("Type for variable '$varName' not found")
    }

    fun getVarTypeOrNull(varName: String): VarDescriptor? {
        return valueMap[varName] ?: functions[varName]
    }

    fun findEnumByEnumerator(name: String): Int? {
        for (enumType in enumTypeMap) {
            if (enumType !is EnumBaseType) {
                continue
            }
            if (enumType.hasEnumerator(name)) {
                return enumType.enumerator(name)
            }
        }
        return null
    }

    fun findEnum(name: String): BaseType? {
        for (enumType in enumTypeMap) {
            if (enumType !is EnumBaseType) {
                continue
            }
            if (enumType.hasEnumerator(name)) {
                return enumType
            }
        }
        return null
    }

    inline fun<reified T: BaseType> getTypeOrNull(name: String): BaseType? = when (T::class) {
        EnumBaseType::class, UncompletedEnumType::class         -> enumTypeMap[name]
        StructBaseType::class, UncompletedStructBaseType::class -> structTypeMap[name]
        UnionBaseType::class, UncompletedUnionBaseType::class   -> unionTypeMap[name]
        else -> null
    }

    fun getTypedefOrNull(name: String): TypeDesc? {
        return typedefs[name]
    }

    fun getTypedef(name: String): TypeDesc {
        return getTypedefOrNull(name) ?: throw Exception("Type for 'typedef $name' not found")
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

    inline fun<reified T: BaseType> getStructType(name: String): BaseType {
        return getTypeOrNull<T>(name) ?: throw Exception("Type for struct $name not found")
    }

    inline fun <reified T : BaseType> addNewType(name: String, type: T): T {
        when (type) {
            is EnumBaseType, is UncompletedEnumType         -> enumTypeMap[name] = type
            is StructBaseType, is UncompletedStructBaseType -> structTypeMap[name] = type
            is UnionBaseType, is UncompletedUnionBaseType   -> unionTypeMap[name] = type
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