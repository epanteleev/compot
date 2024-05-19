package ir.utils

import ir.LocalValue
import ir.module.FunctionData


data class CreationInfoException(override val message: String): Exception(message)

class CreationInfo private constructor(private val creationInfo: Map<LocalValue, Location>) {
    operator fun get(instruction: LocalValue): Location {
        return creationInfo[instruction] ?:
            throw CreationInfoException("value doesn't exist: instruction=$instruction")
    }

    companion object {
        fun create(fd: FunctionData): CreationInfo {
            val creationInfo = hashMapOf<LocalValue, Location>()

            for (arg in fd.arguments()) {
                creationInfo[arg] = Location(fd.blocks.begin(), -1)
            }

            for (bb in fd.blocks) {
                for ((idx, instruction) in bb.iterator().withIndex()) {
                    if (instruction !is LocalValue) {
                        continue
                    }

                    creationInfo[instruction] = Location(bb, idx)
                }
            }

            return CreationInfo(creationInfo)
        }
    }
}