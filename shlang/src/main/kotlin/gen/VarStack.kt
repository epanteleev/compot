package gen

import ir.LocalValue
import types.SpecifiedType


class VarStack {

    private val stack = mutableListOf<MutableMap<String, KeyType>>()

    fun push() {
        stack.add(mutableMapOf())
    }

    fun pop() {
        stack.removeAt(stack.size - 1)
    }

    operator fun set(name: String, type: KeyType) {
        stack.last()[name] = type
    }

    operator fun get(name: String): KeyType? {
        for (i in stack.size - 1 downTo 0) {
            val type = stack[i][name]
            if (type != null) {
                return type
            }
        }
        return null
    }
}

typealias KeyType = Pair<SpecifiedType, LocalValue>