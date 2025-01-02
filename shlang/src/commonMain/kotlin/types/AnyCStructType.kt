package types

import ir.Definitions.QWORD_SIZE


sealed class AnyCStructType(open val name: String,
                            protected val fields: List<Member>,
                            protected val fieldDescriptors: List<FieldDesc>,
                            private val nameToFieldDesc: Map<String, FieldDesc>): CAggregateType() {
    fun fieldByIndexOrNull(name: String): FieldDesc? = nameToFieldDesc[name]

    fun descriptors(): List<FieldDesc> {
        return fieldDescriptors
    }

    fun fieldByName(name: String): FieldDesc {
        return fieldByIndexOrNull(name) ?:
            throw RuntimeException("Cannon find field by name: name=$name, { name=$name, $fields }")
    }

    fun members(): Collection<Member> {
        return fields
    }

    fun isSmall(): Boolean {
        return size() <= QWORD_SIZE * 2
    }

    abstract fun offset(index: Int): Int

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnyCStructType) return false

        if (name != other.name) return false
        if (fields != other.fields) return false

        return true
    }

    final override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + fields.hashCode()
        return result
    }
}