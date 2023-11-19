package ir.instruction

import ir.AnyFunctionPrototype
import ir.Value

interface Callable: Value {
    fun arguments(): Array<Value>
    fun prototype(): AnyFunctionPrototype
    fun shortName(): String {
        return prototype().shortName()
    }

    companion object {
        internal fun isAppropriateTypes(func: AnyFunctionPrototype, args: List<Value>): Boolean {
            for ((expectedType, value) in func.arguments() zip args) {
                if (expectedType != value.type()) {
                    return false
                }
            }

            return true
        }

        fun isCorrect(call: Callable): Boolean {
            return isAppropriateTypes(call.prototype(), call.arguments().toList())
        }
    }
}