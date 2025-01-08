package types

sealed class CUncompletedType: CType()

data class CUncompletedStructType(val name: String): CUncompletedType() {
    override fun size(): Int = throw Exception("Uncompleted type '$name'")
    override fun alignmentOf(): Int = throw Exception("Uncompleted type '$this'")
    override fun toString(): String {
        return "struct $name"
    }
}

data class CUncompletedUnionType(val name: String): CUncompletedType() {
    override fun size(): Int = throw Exception("Uncompleted type")
    override fun alignmentOf(): Int = throw Exception("Uncompleted type '$this'")

    override fun toString(): String {
        return "union $name"
    }
}

data class CUncompletedEnumType(val name: String): CUncompletedType() {
    override fun size(): Int = throw Exception("Uncompleted type") //TODO remove this
    override fun alignmentOf(): Int = TODO("Not yet implemented")
}