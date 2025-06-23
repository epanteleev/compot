package typedesc

interface Scope {
    fun enter()
    fun leave()

    fun<T> scoped(block: () -> T): T {
        enter()
        val ret = block()
        leave()
        return ret
    }
}