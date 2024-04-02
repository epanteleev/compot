package gen


class VarStack<T> {
    private val stack = mutableListOf<MutableMap<String, T>>()

    fun push() {
        stack.add(mutableMapOf())
    }

    fun pop() {
        stack.removeAt(stack.size - 1)
    }

    fun put(name: String, type: T) {
        stack.last()[name] = type
    }

    operator fun get(name: String): T? {
        for (i in stack.size - 1 downTo 0) {
            val type = stack[i][name]
            if (type != null) {
                return type
            }
        }
        return null
    }
}