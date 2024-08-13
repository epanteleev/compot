package ir.module


class ModificationCounter {
    private var controlFlowModificationCounter = 0L
    private var valueModificationCounter = 0L

    fun mutations(): MutationMarker {
        return MutationMarker(controlFlowModificationCounter, valueModificationCounter)
    }

    fun<T> cf(closure: () -> T): T {
        val res = closure()
        controlFlowModificationCounter++
        return res
    }

    fun<T> df(closure: () -> T): T {
        val res = closure()
        valueModificationCounter++
        return res
    }

    fun<T> dfANDcf(closure: () -> T): T {
        val res = closure()
        valueModificationCounter++
        controlFlowModificationCounter++
        return res
    }
}