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

    inline fun cf(closure: () -> Unit) {
        closure()
        incrementControlFlowModifications()
    }

    inline fun df(closure: () -> Unit) {
        closure()
        incrementValueModifications()
    }
}