package types

class TypeHolder(private val valueMap: MutableMap<String, CType>) {
    private val typeMap = hashMapOf<String, BaseType>()
    private val functions = hashMapOf<String, CFunctionType>()

    operator fun get(name: String): CType {
        return getOrNull(name) ?: throw Exception("Type for variable $name not found")
    }

    fun getOrNull(name: String): CType? {
        return valueMap[name]
    }

    fun add(name: String, type: CType) {
        valueMap[name] = type
    }

    fun contains(name: String): Boolean {
        return valueMap.containsKey(name)
    }

    fun getStructType(name: String): BaseType {
        return typeMap[name] ?: throw Exception("Type for struct $name not found")
    }

    fun addStructType(name: String, type: BaseType) {
        typeMap[name] = type
    }

    fun getEnumType(name: String): BaseType {
        return typeMap[name] ?: throw Exception("Type for enum $name not found")
    }

    fun addEnumType(name: String, type: BaseType) {
        typeMap[name] = type
    }

    fun getUnionType(name: String): BaseType {
        return typeMap[name] ?: throw Exception("Type for union $name not found")
    }

    fun addUnionType(name: String, type: BaseType) {
        typeMap[name] = type
    }

    fun getFunctionType(name: String): CType {
        return functions[name] ?: throw Exception("Type for function $name not found")
    }

    fun addFunctionType(name: String, type: CFunctionType) {
        functions[name] = type
    }

    companion object {
        fun default(): TypeHolder {
            val typeMap = hashMapOf<String, CType>()
            return TypeHolder(typeMap)
        }
    }
}