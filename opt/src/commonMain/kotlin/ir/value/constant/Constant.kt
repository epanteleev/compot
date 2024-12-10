package ir.value.constant

import ir.types.*
import ir.value.Value


sealed interface Constant: Value {
    fun data(): String
    override fun type(): Type
}