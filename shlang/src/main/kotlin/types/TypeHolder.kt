package types

class TypeHolder(private val typeMap: MutableMap<String, CType>) {
    operator fun get(name: String): CType {
        return typeMap[name] ?: throw Exception("Type for variable $name not found")
    }

    fun add(name: String, type: CType) {
        typeMap[name] = type
    }

    fun contains(name: String): Boolean {
        return typeMap.containsKey(name)
    }

    fun getStructType(name: String): CType {
        TODO("Not yet implemented")
    }

    fun getEnumType(name: String): CType {
        TODO("Not yet implemented")
    }

    fun getUnionType(name: String): CType {
        TODO("Not yet implemented")
    }

    companion object {
        fun default(): TypeHolder {
            val typeMap = hashMapOf<String, CType>()
            return TypeHolder(typeMap)
        }
    }
}