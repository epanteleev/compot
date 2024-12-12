package codegen

import common.assertion
import ir.types.Type
import ir.Definitions.BYTE_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE
import ir.types.PrimitiveType
import types.AnyCStructType
import types.CAggregateType


internal object CallConvention {
    fun coerceArgumentTypes(cType: AnyCStructType): List<PrimitiveType>? = when (val size = cType.size()) {
        BYTE_SIZE  -> arrayListOf(Type.I8)
        HWORD_SIZE -> arrayListOf(Type.I16)
        HWORD_SIZE + BYTE_SIZE -> arrayListOf(Type.I32)
        WORD_SIZE -> {
            val type = if (cType.hasFloatOnly(0, WORD_SIZE)) Type.F32 else Type.I32
            arrayListOf(type)
        }
        WORD_SIZE + BYTE_SIZE -> {
            val type = if (cType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
            arrayListOf(type)
        }
        QWORD_SIZE -> {
            val type = if (cType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
            arrayListOf(type)
        }
        QWORD_SIZE + BYTE_SIZE  -> {
            val type1 = if (cType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
            arrayListOf(type1, Type.I8)
        }
        QWORD_SIZE + HWORD_SIZE -> {
            val type1 = if (cType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
            arrayListOf(type1, Type.I16)
        }
        QWORD_SIZE + WORD_SIZE  -> {
            val type1 = if (cType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
            val type2 = if (cType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE + WORD_SIZE)) Type.F32 else Type.I32
            arrayListOf(type1, type2)
        }
        QWORD_SIZE * 2          -> {
            val type1 = if (cType.hasFloatOnly(0, QWORD_SIZE)) Type.F64 else Type.I64
            val type2 = if (cType.hasFloatOnly(QWORD_SIZE, QWORD_SIZE * 2)) Type.F64 else Type.I64
            arrayListOf(type1, type2)
        }
        else -> {
            assertion(!cType.isSmall()) { "unsupported size=$size" }
            null
        }
    }
}