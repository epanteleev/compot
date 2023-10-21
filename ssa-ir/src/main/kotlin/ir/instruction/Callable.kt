package ir.instruction

import ir.AnyFunctionPrototype
import ir.Value

interface Callable: Value {
    fun arguments(): Array<Value>
    fun prototype(): AnyFunctionPrototype
    fun shortName(): String {
        return prototype().shortName()
    }
}