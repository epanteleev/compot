package gen

import ir.types.Type
import ir.Definitions.BYTE_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE
import ir.types.NonTrivialType
import ir.types.StructType


object CallConvention {
    fun coerceArgumentTypes(cType: StructType): List<NonTrivialType>? = when (cType.sizeOf()) {
        BYTE_SIZE  -> arrayListOf(Type.I8)
        HWORD_SIZE -> arrayListOf(Type.I16)
        WORD_SIZE  -> {
            val type = if (cType.hasFloatOnly(0, WORD_SIZE)) Type.F32 else Type.I32
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
        else -> null
    }
}