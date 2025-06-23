package types

import ir.Definitions.BYTE_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE

sealed class AnyCSigned: AnyCInteger()

data object CHAR: AnyCSigned() {
    override fun toString(): String = "char"
    override fun size(): Int = BYTE_SIZE
}

data object SHORT: AnyCSigned() {
    override fun toString(): String = "short"
    override fun size(): Int = HWORD_SIZE
}

data object INT: AnyCSigned() {
    override fun toString(): String = "int"
    override fun size(): Int = WORD_SIZE
}

data object LONG: AnyCSigned() {
    override fun toString(): String = "long"
    override fun size(): Int = QWORD_SIZE
}