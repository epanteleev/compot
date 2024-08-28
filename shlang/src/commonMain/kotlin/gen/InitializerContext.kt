package gen

import ir.value.Value
import types.CType


class InitializerContext {
    private val valueStack = arrayListOf<Value>()
    private val typeStack = arrayListOf<CType>()
    private val indexStack = arrayListOf<Int>()

    private fun push(value: Value, type: CType) {
        valueStack.add(value)
        typeStack.add(type)
    }

    private fun pop() {
        valueStack.removeAt(valueStack.size - 1)
        typeStack.removeAt(typeStack.size - 1)
    }

    fun scope(value: Value, type: CType, block: () -> Unit) {
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

    fun peekType(): CType {
        return typeStack[typeStack.size - 1]
    }

    fun peekIndex(): Int {
        return indexStack[indexStack.size - 1]
    }
}