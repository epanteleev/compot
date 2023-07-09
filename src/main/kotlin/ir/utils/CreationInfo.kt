package ir.utils

import ir.*

class CreationInfo private constructor(private val creationInfo: MutableMap<Instruction, Location>) {
    fun get(instruction: Instruction): Location {
        return creationInfo[instruction]!!
    }

    companion object {
        fun create(basicBlocks: BasicBlocks): CreationInfo {
            val creationInfo = hashMapOf<Instruction, Location>()

            for (bb in basicBlocks) {
                for ((idx, instruction) in bb.withIndex()) {
                    if (instruction is Store || instruction is TerminateInstruction) {
                        continue
                    }

                    creationInfo[instruction] = Location(bb, idx)
                }
            }

            return CreationInfo(creationInfo)
        }
    }
}