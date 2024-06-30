package gen

import ir.Value


class VarStack {
    private val stack = mutableListOf<MutableMap<String, Value>>()

    fun push() {
        stack.add(mutableMapOf())
    }

    fun pop() {
        stack.removeAt(stack.size - 1)
    }

    inline fun<T> scoped(block: () -> T): T {
        push()
        val ret = block()
        pop()
        return ret
    }

    operator fun set(name: String, type: Value) {
        stack.last()[name] = type
    }

    operator fun get(name: String): Value? {
        for (i in stack.size - 1 downTo 0) {
            val type = stack[i][name]
            if (type != null) {
                return type
            }
        }
        return null
    }
}
