package ir.utils

import ir.*

class CreationInfo private constructor(private val creationInfo: MutableMap<ValueInstruction, Location>) {
    fun get(instruction: ValueInstruction): Location {
        return creationInfo[instruction]!!
    }

    companion object {
        fun create(basicBlocks: BasicBlocks): CreationInfo {
            val creationInfo = hashMapOf<ValueInstruction, Location>()

            for (bb in basicBlocks) {
                for ((idx, instruction) in bb.withIndex()) {
                    if (instruction !is ValueInstruction) {
                        continue
                    }
                    if (instruction.type() == Type.Void) {
                        continue
                    }

                    creationInfo[instruction] = Location(bb, idx)
                }
            }

            return CreationInfo(creationInfo)
        }
    }
}