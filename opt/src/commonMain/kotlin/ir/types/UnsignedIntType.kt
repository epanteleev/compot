package ir.types

import ir.Definitions.BYTE_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE


sealed class UnsignedIntType : IntegerType()

object U8Type : UnsignedIntType() {
    override fun sizeOf(): Int = BYTE_SIZE
    override fun toString(): String = "u8"
}

object U16Type : UnsignedIntType() {
    override fun sizeOf(): Int = HWORD_SIZE
    override fun toString(): String = "u16"
}

object U32Type : UnsignedIntType() {
    override fun sizeOf(): Int = WORD_SIZE
    override fun toString(): String = "u32"
}

object U64Type : UnsignedIntType() {
    override fun sizeOf(): Int = QWORD_SIZE
    override fun toString(): String = "u64"
}