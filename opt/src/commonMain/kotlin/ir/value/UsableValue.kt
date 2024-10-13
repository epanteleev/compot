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

    fun name(): String

    companion object {
        fun<V: Value> updateUsages(localValue: UsableValue, replacement: () -> V): V {
            val valueToReplace = replacement()
            for (user in localValue.release()) {
                for ((idxUse, use) in user.operands().withIndex()) {
                    if (use !== localValue) {
                        continue
                    }
                    // New value can use the old value
                    if (user == valueToReplace) {
                        continue
                    }

                    user.update(idxUse, valueToReplace)
                }
            }
            return valueToReplace
        }
    }
}