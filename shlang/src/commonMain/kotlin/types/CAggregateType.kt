package types


sealed class CAggregateType: CType()

sealed class AnyStructType(open val name: String): CAggregateType() {
    protected val fields = arrayListOf<Pair<String, TypeDesc>>()
    override fun typename(): String = name

    fun fieldIndex(name: String): Int {
        return fields.indexOfFirst { it.first == name }
    }

    fun fieldIndex(index: Int): TypeDesc {
        return fields[index].second
    }

    fun fields(): List<Pair<String, TypeDesc>> {
        return fields
    }

    //TODO avoid???
    internal fun addField(name: String, type: TypeDesc) {
        fields.add(name to type)
    }
}

data class CStructType(override val name: String): AnyStructType(name) { //TODO
    override fun size(): Int {
        return fields.sumOf { it.second.size() }
    }

    override fun toString(): String = buildString {
        append("struct $name")
        append(" {")
        fields.forEach { (name, type) ->
            append("$type $name;")
        }
        append("}")
    }
}

data class CUnionType(override val name: String): AnyStructType(name) {
    override fun size(): Int {
        if (fields.isEmpty()) {
            return 0
        }
        return fields.maxOf { it.second.size() }
    }

    override fun toString(): String = buildString {
        append("union $name")
        append(" {")
        fields.forEach { (name, type) ->
            append("$type $name;")
        }
        append("}")
    }
}

data class CEnumType(val name: String, private val enumerators: Map<String, Int>): CPrimitive() {
    override fun typename(): String = name

    override fun size(): Int {
        return INT.size()
    }

    fun hasEnumerator(name: String): Boolean {
        return enumerators.contains(name)
    }

    fun enumerator(name: String): Int? {
        return enumerators[name] //TODO temporal
    }
}

sealed class AnyCArrayType(val type: TypeDesc): CAggregateType() {
    fun element(): TypeDesc = type
}

class CArrayType(type: TypeDesc, val dimension: Long) : AnyCArrayType(type) {
    override fun typename(): String {
        return toString()
    }

    override fun size(): Int {
        return type.size() * dimension.toInt() //TODO
    }

    override fun toString(): String = buildString {
        append("[$dimension]")
        append(type)
    }
}