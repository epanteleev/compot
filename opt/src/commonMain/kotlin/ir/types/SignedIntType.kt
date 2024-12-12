package ir.types

import ir.Definitions.BYTE_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE


sealed class SignedIntType(private val size: Int) : IntegerType {
    override fun sizeOf(): Int {
        return size
    }

    override fun toString(): String {
        return when (size) {
            1 -> "i8"
            2 -> "i16"
            4 -> "i32"
            8 -> "i64"
            else -> throw TypeErrorException("unsupported size=$size")
        }
    }
}

object I8Type : SignedIntType(BYTE_SIZE)
object I16Type : SignedIntType(HWORD_SIZE)
object I32Type : SignedIntType(WORD_SIZE)
object I64Type : SignedIntType(QWORD_SIZE)