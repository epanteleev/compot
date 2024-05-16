package ir.utils

import ir.instruction.Instruction
import ir.instruction.ValueInstruction
import ir.module.BasicBlocks


class DefUseInfo private constructor() {
    fun isNotUsed(v: Instruction): Boolean {
        if (v is ValueInstruction) {
            return v.usedIn().isEmpty()
        }
        return true
    }

    fun usedIn(v: Instruction): List<Instruction> {
        if (v is  ValueInstruction) {
            return v.usedIn()
        }

        return arrayListOf()
    }

    companion object {
        fun create(basicBlocks: BasicBlocks): DefUseInfo {
            return DefUseInfo()
        }
    }
}