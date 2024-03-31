package types

class TypeHolder(private val typeMap: MutableMap<String, Type>) {
    fun get(name: String): Type {
        return typeMap[name] ?: throw Exception("Type $name not found")
    }

    fun add(name: String, type: Type) {
        typeMap[name] = type
    }

    fun contains(name: String): Boolean {
        return typeMap.containsKey(name)
    }

    fun getStructType(name: String): Type {
        TODO("Not yet implemented")
    }

    fun getEnumType(name: String): Type {
        TODO("Not yet implemented")
    }

    fun getUnionType(name: String): Type {
        TODO("Not yet implemented")
    }

    companion object {
        fun default(): TypeHolder {
            val typeMap = hashMapOf<String, Type>(
                "int" to Type.INT,
                "char" to Type.CHAR,
                "void" to Type.VOID,
                "float" to Type.FLOAT,
                "double" to Type.DOUBLE,
                "long" to Type.LONG,
                "short" to Type.SHORT,
                "signed" to Type.INT,
                "unsigned" to Type.UINT,
                "bool" to Type.BOOL,
            )
            return TypeHolder(typeMap)
        }
    }
}