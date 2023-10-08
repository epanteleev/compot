package ir.instruction

import ir.AnyFunctionPrototype
import ir.Value

interface Callable: Value {
    fun arguments(): List<Value>
    fun prototype(): AnyFunctionPrototype
}