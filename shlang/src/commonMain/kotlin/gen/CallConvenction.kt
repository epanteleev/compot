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
        WORD_SIZE  -> arrayListOf(Type.I32)
        QWORD_SIZE -> arrayListOf(Type.I64)
        QWORD_SIZE + BYTE_SIZE  -> arrayListOf(Type.I64, Type.I8)
        QWORD_SIZE + HWORD_SIZE -> arrayListOf(Type.I64, Type.I16)
        QWORD_SIZE + WORD_SIZE  -> {
            val type1 = if (cType.hasFloatOnly(0, 8)) Type.F64 else Type.I64
            val type2 = if (cType.hasFloatOnly(8, 12)) Type.F32 else Type.I32
            arrayListOf(type1, type2)
        }
        QWORD_SIZE * 2          -> arrayListOf(Type.I64, Type.I64)
        else -> null
    }
}