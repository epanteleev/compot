package ir.utils

import ir.instruction.Instruction
import ir.module.BasicBlocks

class DefUseInfo private constructor(private val defUse: MutableMap<Instruction, MutableList<Instruction>>) {
    private fun addUsages(instruction: Instruction, usages: List<Instruction>) {
        for (v in usages) {
            (defUse.getOrPut(v) { arrayListOf() }).add(instruction)
        }
    }

    fun isNotUsed(v: Instruction): Boolean {
        return defUse[v] == null
    }

    fun usedIn(v: Instruction): List<Instruction> {
        return defUse[v] as List<Instruction>
    }

    companion object {
        fun create(basicBlocks: BasicBlocks): DefUseInfo {
            val defUseInfo = DefUseInfo(hashMapOf())
            for (bb in basicBlocks) {
                for (instruction in bb) {
                    defUseInfo.addUsages(instruction, instruction.usedInstructions())
                }
            }

            return defUseInfo
        }
    }
}