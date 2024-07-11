package types


class TypeHolder(private val valueMap: MutableMap<String, CType>): Scope {
    private val typeMap = arrayListOf(hashMapOf<String, BaseType>()) //TODO separate holder for struct, enum, union.
    private val functions = hashMapOf<String, CFunctionType>()
    private val typedefs = arrayListOf(hashMapOf<String, CType>())

    operator fun get(varName: String): CType {
        return valueMap[varName] ?: throw Exception("Type for variable '$varName' not found")
    }

    fun getTypeOrNull(name: String): BaseType? {
        for (i in typeMap.size - 1 downTo 0) {
            val type = typeMap[i][name]
            if (type != null) {
                return type
            }
        }
        return null
    }

    fun getTypedefOrNull(name: String): CType? {
        for (i in typedefs.size - 1 downTo 0) {
            val type = typedefs[i][name]
            if (type != null) {
                return type
            }
        }
        return null
    }

    fun getTypedef(name: String): CType {
        return getTypedefOrNull(name) ?: throw Exception("Type for 'typedef $name' not found")
    }

    fun addTypedef(name: String, type: CType): CType {
        typedefs.last()[name] = type
        return type
    }

    fun addVar(name: String, type: CType) {
        valueMap[name] = type
    }

    fun containsVar(varName: String): Boolean {
        return valueMap.containsKey(varName)
    }

    fun getStructType(name: String): BaseType {
        return getTypeOrNull(name) ?: throw Exception("Type for struct $name not found")
    }

    fun <T : BaseType> addStructType(name: String, type: T): T {
        typeMap.last()[name] = type
        return type
    }

    fun getEnumType(name: String): BaseType {
        return getTypeOrNull(name) ?: throw Exception("Type for enum $name not found")
    }

    fun addEnumType(name: String, type: EnumBaseType): EnumBaseType {
        typeMap.last()[name] = type
        return type
    }

    fun addEnumType(name: String, type: UncompletedEnumType): UncompletedEnumType {
        typeMap.last()[name] = type
        return type
    }

    fun getUnionType(name: String): BaseType {
        return getTypeOrNull(name) ?: throw Exception("Type for union $name not found")
    }

    fun addUnionType(name: String, type: BaseType) {
        typeMap.last()[name] = type
    }

    fun getFunctionType(name: String): CType {
        return functions[name] ?: let {
            throw Exception("Type for function '$name' not found")
        }
    }

    fun addFunctionType(name: String, type: CFunctionType) {
        functions[name] = type
    }

    override fun enter() {
        typeMap.add(hashMapOf())
    }

    override fun leave() {
        typeMap.removeLast()
    }

    companion object {
        fun default(): TypeHolder {
            val typeMap = hashMapOf<String, CType>()
            return TypeHolder(typeMap)
        }
    }
}