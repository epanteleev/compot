package types

class InitializerType(val initializer: List<CType>): CType() {
    override fun typename(): String = buildString {
        append("{")
        initializer.forEachIndexed { index, type ->
            append(type)
            if (index < initializer.size - 1) append(", ")
        }
        append("}")
    }

    override fun size(): Int = initializer.fold(0) { acc, type -> acc + type.size() }
}