package types

class InitializerType(private val initializer: List<CType>): CAggregateType() {
    override fun typename(): String = buildString {
        append("{")
        initializer.forEachIndexed { index, type ->
            append(type)
            if (index < initializer.size - 1) append(", ")
        }
        append("}")
    }

    override fun size(): Int = initializer.fold(0) { acc, type -> acc + type.size() }
    override fun alignmentOf(): Int = initializer.fold(1) { acc, type -> maxOf(acc, type.alignmentOf()) } //TODO check this
}