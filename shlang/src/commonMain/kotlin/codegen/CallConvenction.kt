package codegen

import common.assertion
import ir.types.*
import ir.Definitions.BYTE_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE
import types.AnyCStructType


internal object CallConvention {
    fun coerceArgumentTypes(cType: AnyCStructType): List<PrimitiveType>? = when (val size = cType.size()) {
        BYTE_SIZE  -> arrayListOf(I8Type)
        HWORD_SIZE -> arrayListOf(I16Type)
        HWORD_SIZE + BYTE_SIZE -> arrayListOf(I32Type)
        WORD_SIZE -> {
            val type = if (cType.hasFloatOnly(0, WORD_SIZE)) F32Type else I32Type
            arrayListOf(type)
        }
        WORD_SIZE + BYTE_SIZE -> {
            val type = if (cType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
            arrayListOf(type)
        }
        QWORD_SIZE -> {
            val type = if (cType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
            arrayListOf(type)
        }
        QWORD_SIZE + BYTE_SIZE  -> {
            val type1 = if (cType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
            arrayListOf(type1, I8Type)
        }
        QWORD_SIZE + HWORD_SIZE -> {
            val type1 = if (cType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
            arrayListOf(type1, I16Type)
        }
        QWORD_SIZE + WORD_SIZE  -> {
            val type1 = if (cType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
            val type2 = if (cType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE + WORD_SIZE)) F32Type else I32Type
            arrayListOf(type1, type2)
        }
        QWORD_SIZE * 2          -> {
            val type1 = if (cType.hasFloatOnly(0, QWORD_SIZE)) F64Type else I64Type
            val type2 = if (cType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE * 2)) F64Type else I64Type
            arrayListOf(type1, type2)
        }
        else -> {
            assertion(!cType.isSmall()) { "unsupported size=$size" }
            null
        }
    }
}