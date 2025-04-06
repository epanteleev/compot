package codegen

import typedesc.Scope

class VarStack<V>: Scope, Iterable<V> {
    private val stack = mutableListOf<MutableMap<String, V>>(hashMapOf())

    override fun enter() {
        stack.add(hashMapOf())
    }

    override fun leave() {
        stack.removeLast()
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

    override fun iterator(): Iterator<V> {
        return VarStackIterator(stack)
    }

    fun copy(): VarStack<V> {
        val newStack = VarStack<V>()
        for (i in stack.indices) {
            newStack.stack[i] = HashMap(stack[i])
        }

        return newStack
    }

    private class VarStackIterator<V>(private val stack: List<MutableMap<String, V>>) : Iterator<V> {
        private var index = 0
        private var iterator: Iterator<V>? = null

        override fun hasNext(): Boolean {
            while (index < stack.size) {
                if (iterator == null) {
                    iterator = stack[index].values.iterator()
                }
                if (iterator!!.hasNext()) {
                    return true
                }
                index++
                iterator = null
            }
            return false
        }

        override fun next(): V {
            if (!hasNext()) {
                throw NoSuchElementException()
            }
            return iterator!!.next()
        }
    }
}
