package types

import ir.Definitions
import typedesc.TypeDesc


class CStructType private constructor(
    name: String,
    fields: List<Member>,
    fieldDescriptors: List<FieldDesc>,
    nameToFieldDesc: Map<String, FieldDesc>,
    private val alignments: IntArray
) : AnyCStructType(name, fields, fieldDescriptors, nameToFieldDesc) {
    private val maxAlignment by lazy { alignments.maxOrNull() ?: 1 }

    override fun alignmentOf(): Int = maxAlignment

    override fun size(): Int {
        if (fieldDescriptors.isEmpty()) {
            return 1
        }
        var offset = 0
        for (idx in fieldDescriptors.indices) {
            offset = Definitions.alignTo(offset + fieldDescriptors[idx].size(), alignments[idx])
        }
        return Definitions.alignTo(offset, alignmentOf())
    }

    override fun offset(index: Int): Int {
        var current = 0
        for (i in 0 until index) {
            current = Definitions.alignTo(current + fieldDescriptors[i].size(), alignments[i])
        }
        return Definitions.alignTo(current, alignments[index])
    }

    override fun toString(): String = buildString {
        append("struct $name")
        append(" {")
        fields.joinTo(this, separator = "") { field -> field.toString() }
        append("}")
    }

    fun fieldByNameOrNull(index: Int): TypeDesc? {
        if (index < 0 || index >= fieldDescriptors.size) {
            return null
        }

        return fieldDescriptors[index].member.typeDesc()
    }

    companion object {
        private fun evaluateFieldDescriptors(fields: List<Member>): List<FieldDesc> {
            val fieldDescs = arrayListOf<FieldDesc>()
            var offset = 0
            for ((idx, field) in fields.withIndex()) {
                when (field) {
                    is FieldMember -> {
                        val fieldDesc = FieldDesc(field.name, idx + offset, field)
                        fieldDescs.add(fieldDesc)
                    }
                    is AnonMember -> when (val cType = field.cType()) {
                        is CUnionType -> {
                            for (fieldDesc in cType.descriptors()) {
                                val name = fieldDesc.name()
                                val newFieldDesc = FieldDesc(name, idx + fieldDesc.index + offset, field)
                                fieldDescs.add(newFieldDesc)
                            }
                        }
                        is CStructType -> {
                            for (fieldDesc in cType.descriptors()) {
                                val name = fieldDesc.name()
                                val newFieldDesc = FieldDesc(name, idx + fieldDesc.index + offset, field)
                                fieldDescs.add(newFieldDesc)
                                offset += cType.members().size
                            }
                        }
                    }
                }
            }
            return fieldDescs
        }

        private fun alignments(fields: List<FieldDesc>): IntArray {
            var current = 0
            val result = IntArray(fields.size)
            for (i in fields.indices) {
                val field = fields[i]
                val alignment = field.cType().alignmentOf()
                current = Definitions.alignTo(current + field.cType().size(), alignment)
                result[i] = alignment
            }
            return result
        }

        fun create(name: String, fields: List<Member>): CStructType {
            val fieldDescs = evaluateFieldDescriptors(fields)
            val nameToFieldDesc = fieldDescs.associateByTo(hashMapOf()) { it.name() }
            return CStructType(name, fields, fieldDescs, nameToFieldDesc, alignments(fieldDescs))
        }
    }
}