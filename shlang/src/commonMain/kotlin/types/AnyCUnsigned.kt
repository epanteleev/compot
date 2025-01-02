package types

import ir.Definitions.BYTE_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE


sealed class AnyCUnsigned: AnyCInteger()

data object UCHAR: AnyCUnsigned() {
    override fun toString(): String = "unsigned char"
    override fun size(): Int = BYTE_SIZE
}

data object USHORT: AnyCUnsigned() {
    override fun toString(): String = "unsigned short"
    override fun size(): Int = HWORD_SIZE
}

data object UINT: AnyCUnsigned() {
    override fun toString(): String = "unsigned int"
    override fun size(): Int = WORD_SIZE
}

data object ULONG: AnyCUnsigned() {
    override fun toString(): String = "unsigned long"
    override fun size(): Int = QWORD_SIZE
}