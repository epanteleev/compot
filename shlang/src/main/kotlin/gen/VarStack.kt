package gen

import types.SpecifiedType

class VarStack {
    private val stack = mutableListOf<MutableMap<String, SpecifiedType>>()

    fun push() {
        stack.add(mutableMapOf())
    }

    fun pop() {
        stack.removeAt(stack.size - 1)
    }

    fun put(name: String, type: SpecifiedType) {
        stack.last()[name] = type
    }

    operator fun get(name: String): SpecifiedType? {
        for (i in stack.size - 1 downTo 0) {
            val type = stack[i][name]
            if (type != null) {
                return type
            }
        }
        return null
    }
}