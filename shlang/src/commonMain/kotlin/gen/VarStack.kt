package gen

import ir.LocalValue


class VarStack {

    private val stack = mutableListOf<MutableMap<String, LocalValue>>()

    fun push() {
        stack.add(mutableMapOf())
    }

    fun pop() {
        stack.removeAt(stack.size - 1)
    }

    operator fun set(name: String, type: LocalValue) {
        stack.last()[name] = type
    }

    operator fun get(name: String): LocalValue? {
        for (i in stack.size - 1 downTo 0) {
            val type = stack[i][name]
            if (type != null) {
                return type
            }
        }
        return null
    }
}
