package types

class TypeHolder(private val typeMap: MutableMap<String, CType>) {
    operator fun get(name: String): CType {
        return typeMap[name] ?: throw Exception("Type $name not found")
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
            val typeMap = hashMapOf<String, CType>(
                "int" to CType.INT,
                "char" to CType.CHAR,
                "void" to CType.VOID,
                "float" to CType.FLOAT,
                "double" to CType.DOUBLE,
                "long" to CType.LONG,
                "short" to CType.SHORT,
                "signed" to CType.INT,
                "unsigned" to CType.UINT,
                "bool" to CType.BOOL,
            )
            return TypeHolder(typeMap)
        }
    }
}