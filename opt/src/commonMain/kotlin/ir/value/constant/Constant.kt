package ir.value.constant

import ir.types.*
import ir.value.Value


sealed interface Constant: Value {
    override fun type(): Type
}