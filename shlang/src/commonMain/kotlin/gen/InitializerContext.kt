package gen

import ir.value.Value
import types.TypeDesc


class InitializerContext {
    private val valueStack = arrayListOf<Value>()
    private val typeStack = arrayListOf<TypeDesc>()
    private val indexStack = arrayListOf<Int>()

    private fun push(value: Value, type: TypeDesc) {
        valueStack.add(value)
        typeStack.add(type)
    }

    private fun pop() {
        valueStack.removeAt(valueStack.size - 1)
        typeStack.removeAt(typeStack.size - 1)
    }

    fun scope(value: Value, type: TypeDesc, block: () -> Unit) {
        push(value, type)
        block()
        pop()
    }

    fun withIndex(index: Int, block: () -> Unit) {
        indexStack.add(index)
        block()
        indexStack.removeAt(indexStack.size - 1)
    }

    fun peekValue(): Value {
        return valueStack[valueStack.size - 1]
    }

    fun peekType(): TypeDesc {
        return typeStack[typeStack.size - 1]
    }

    fun peekIndex(): Int {
        return indexStack[indexStack.size - 1]
    }
}