package ir.instruction

import ir.value.Value

sealed interface IndirectionCallable: Callable {
    fun pointer(): Value
    fun pointer(newValue: Value)
}