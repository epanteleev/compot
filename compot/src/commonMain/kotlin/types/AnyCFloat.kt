package types

import ir.Definitions.DOUBLE_SIZE
import ir.Definitions.FLOAT_SIZE

sealed class AnyCFloat: CPrimitive()

data object FLOAT: AnyCFloat() {
    override fun toString(): String = "float"
    override fun size(): Int = FLOAT_SIZE
}

data object DOUBLE: AnyCFloat() {
    override fun toString(): String = "double"
    override fun size(): Int = DOUBLE_SIZE
}