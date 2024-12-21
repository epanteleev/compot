package ir.types

import ir.Definitions.BYTE_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE


sealed class SignedIntType: IntegerType()

object I8Type : SignedIntType() {
    override fun sizeOf(): Int = BYTE_SIZE
    override fun toString(): String = "i8"
}

object I16Type : SignedIntType() {
    override fun sizeOf(): Int = HWORD_SIZE
    override fun toString(): String = "i16"
}

object I32Type : SignedIntType() {
    override fun sizeOf(): Int = WORD_SIZE
    override fun toString(): String = "i32"
}

object I64Type : SignedIntType() {
    override fun sizeOf(): Int = QWORD_SIZE
    override fun toString(): String = "i64"
}