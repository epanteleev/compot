package types

import ir.Definitions
import ir.Definitions.BYTE_SIZE
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
        if (fields.isEmpty()) {
            return BYTE_SIZE
        }
        var offset = 0
        for (idx in fields.indices) {
            offset = Definitions.alignTo(offset + fields[idx].size(), alignments[idx])
        }
        return Definitions.alignTo(offset, alignmentOf())
    }

    override fun offset(index: Int): Int {
        var current = 0
        for (i in 0 until index) {
            current = Definitions.alignTo(current + fields[i].size(), alignments[i])
        }
        return Definitions.alignTo(current, alignments[index])
    }

    override fun toString(): String = buildString {
        append("struct $name")
        append(" {")
        fields.joinTo(this, separator = "") { field -> field.toString() }
        append("}")
    }

    fun fieldByIndexOrNull(index: Int): TypeDesc? {
        if (index < 0 || index >= fieldDescriptors.size) {
            return null
        }

        return fieldDescriptors[index].typeDesc()
    }

    fun fieldByIndex(index: Int): TypeDesc {
        return fieldByIndexOrNull(index) ?: throw RuntimeException("Cannot find field by index: index=$index, { name=$name, $fields }")
    }

    companion object {
        private fun evaluateFieldDescriptors(fields: List<Member>): List<FieldDesc> {
            val fieldDescs = arrayListOf<FieldDesc>()
            var offset = 0
            for ((idx, field) in fields.withIndex()) {
                when (field) {
                    is FieldMember -> fieldDescs.add(FieldDesc(field.name, idx + offset, field))
                    is AnonMember -> when (val cType = field.cType()) {
                        is CUnionType -> {
                            for (fieldDesc in cType.descriptors()) {
                                val name = fieldDesc.name()
                                val newFieldDesc = FieldDesc(name, idx + fieldDesc.index + offset, fieldDesc.member())
                                fieldDescs.add(newFieldDesc)
                            }
                        }
                        is CStructType -> {
                            for (fieldDesc in cType.descriptors()) {
                                val name = fieldDesc.name()
                                val newFieldDesc = FieldDesc(name, idx + fieldDesc.index + offset, fieldDesc.member())
                                fieldDescs.add(newFieldDesc)
                                offset += cType.members().size
                            }
                        }
                    }
                }
            }
            return fieldDescs
        }

        private fun alignments(fields: List<Member>): IntArray {
            var current = 0
            val result = IntArray(fields.size)
            for (i in fields.indices) {
                val field = fields[i]
                val alignment = field.alignmentOf()
                current = Definitions.alignTo(current + field.cType().size(), alignment)
                result[i] = alignment
            }
            return result
        }

        fun create(name: String, fields: List<Member>): CStructType {
            val fieldDescriptors = evaluateFieldDescriptors(fields)
            val nameToFieldDesc = fieldDescriptors.associateByTo(hashMapOf()) { it.name() }
            return CStructType(name, fields, fieldDescriptors, nameToFieldDesc, alignments(fields))
        }
    }
}