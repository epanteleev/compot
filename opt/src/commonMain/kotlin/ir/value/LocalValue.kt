package ir.value

import ir.instruction.Instruction


interface LocalValue: Value {
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
}