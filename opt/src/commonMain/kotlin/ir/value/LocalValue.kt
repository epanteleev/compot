package ir.value

interface LocalValue: UsableValue {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}