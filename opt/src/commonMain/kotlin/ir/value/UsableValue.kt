package ir.value

import ir.instruction.Instruction


interface UsableValue: Value {
    var usedIn: MutableList<Instruction>

    fun addUser(instruction: Instruction) {
        usedIn.add(instruction)
    }

    fun killUser(instruction: Instruction) {
        usedIn.remove(instruction)
    }

    fun release(): List<Instruction> {
        val result = usedIn
        usedIn = arrayListOf()
        return result
    }

    fun usedIn(): List<Instruction> {
        return usedIn
    }

    fun<V: Value> updateUsages(replacement: V): V {
        for (user in release()) {
            for ((idxUse, use) in user.operands().withIndex()) {
                if (use !== this) {
                    continue
                }
                // New value can use the old value
                if (user == replacement) {
                    continue
                }

                user.update(idxUse, replacement)
            }
        }
        return replacement
    }

    fun name(): String

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}