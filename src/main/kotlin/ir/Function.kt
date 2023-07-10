package ir

class Function(val index: Int, val name: String, private val returnType: Type) {
    fun type(): Type {
        return returnType
    }

    override fun hashCode(): Int {
        return index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Function

        if (index != other.index) return false
        if (name != other.name) return false
        return returnType == other.returnType
    }
}

class ExternFunction(val name: String, private val returnType: Type, val arguments: List<Type>) {
    fun type(): Type {
        return returnType
    }
}