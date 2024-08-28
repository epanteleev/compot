package ir.module

class ModificationCounter {
    private var controlFlowModificationCounter = 0
    private var valueModificationCounter = 0

    fun controlFlowModifications(): Int {
        return controlFlowModificationCounter
    }

    fun valueModifications(): Int {
        return valueModificationCounter
    }

    fun incrementControlFlowModifications() {
        controlFlowModificationCounter++
    }

    fun incrementValueModifications() {
        valueModificationCounter++
    }

    fun<T> cf(closure: () -> T): T {
        val res = closure()
        incrementControlFlowModifications()
        return res
    }

    fun<T> df(closure: () -> T): T {
        val res = closure()
        incrementValueModifications()
        return res
    }

    fun<T> dfANDcf(closure: () -> T): T {
        val res = closure()
        incrementValueModifications()
        incrementControlFlowModifications()
        return res
    }
}