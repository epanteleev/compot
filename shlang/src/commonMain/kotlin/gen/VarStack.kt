package gen

import types.Scope

class VarStack<V>: Scope {
    private val stack = mutableListOf<MutableMap<String, V>>(hashMapOf())

    fun containsKey(name: String): Boolean {
        for (i in stack.size - 1 downTo 0) {
            if (stack[i].containsKey(name)) {
                return true
            }
        }
        return false
    }

    override fun enter() {
        stack.add(hashMapOf())
    }

    override fun leave() {
        stack.removeAt(stack.size - 1)
    }

    operator fun set(name: String, type: V) {
        stack.last()[name] = type
    }

    operator fun get(name: String): V? {
        for (i in stack.size - 1 downTo 0) {
            val type = stack[i][name]
            if (type != null) {
                return type
            }
        }
        return null
    }
}
