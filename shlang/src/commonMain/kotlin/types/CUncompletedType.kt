package types

sealed class CUncompletedType: CType()

data class CUncompletedStructType(val name: String): CUncompletedType() {
    override fun toString(): String {
        return "struct $name"
    }
}

data class CUncompletedUnionType(val name: String): CUncompletedType() {
    override fun toString(): String {
        return "union $name"
    }
}

data class CUncompletedEnumType(val name: String): CUncompletedType()