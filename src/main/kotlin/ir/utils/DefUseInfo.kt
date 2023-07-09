package ir.utils

import ir.BasicBlocks
import ir.Value

class DefUseInfo private constructor(private val defUse: MutableMap<Value, MutableList<Value>>) {
    private fun addUsages(instruction: Value, usages: List<Value>) {
        for (v in usages) {
            (defUse.getOrPut(v) { arrayListOf() }).add(instruction)
        }
    }

    fun isNotUsed(v: Value): Boolean {
        return defUse[v] == null
    }

    fun usedIn(v: Value): List<Value> {
        return defUse[v] as List<Value>
    }

    companion object {
        fun create(basicBlocks: BasicBlocks): DefUseInfo {
            val defUseInfo = DefUseInfo(hashMapOf())
            for (bb in basicBlocks) {
                for (instruction in bb) {
                    defUseInfo.addUsages(instruction, instruction.usages)
                }
            }

            return defUseInfo
        }
    }
}