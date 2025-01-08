package types

import ir.Definitions.BYTE_SIZE


class CUnionType private constructor(
    override val name: String,
    fields: List<Member>,
    fieldDescriptors: List<FieldDesc>,
    nameToFieldDesc: Map<String, FieldDesc>
) : AnyCStructType(name, fields, fieldDescriptors, nameToFieldDesc) {
    override fun size(): Int {
        if (fields.isEmpty()) {
            return BYTE_SIZE
        }

        return fields.maxOf { it.cType().size() }
    }

    override fun toString(): String = buildString {
        append("union $name")
        append(" {")
        fields.joinTo(this, separator = "") { field -> field.toString() }
        append("}")
    }

    override fun offset(index: Int): Int {
        return 0
    }

    override fun alignmentOf(): Int {
        return fields.maxOf { it.cType().alignmentOf() }
    }

    companion object {
        private fun evaluateFieldDescriptors(fields: List<Member>): List<FieldDesc> {
            val fieldDesc = arrayListOf<FieldDesc>()

            for (field in fields) {
                when (field) {
                    is FieldMember -> {
                        fieldDesc.add(FieldDesc(field.name, 0, field))
                    }
                    is AnonMember -> {
                        val cType = field.cType()
                        fieldDesc.addAll(cType.descriptors())
                    }
                }
            }
            return fieldDesc
        }

        fun create(name: String, fields: List<Member>): CUnionType {
            val fieldDesc = evaluateFieldDescriptors(fields)
            val nameToFieldDesc = fieldDesc.associateByTo(hashMapOf()) { it.name() }
            return CUnionType(name, fields, fieldDesc, nameToFieldDesc)
        }
    }
}