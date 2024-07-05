package ir.utils

import ir.value.LocalValue
import ir.module.FunctionData


data class CreationInfoException(override val message: String): Exception(message)

class CreationInfo private constructor(private val creationInfo: Map<_root_ide_package_.ir.value.LocalValue, Location>) {
    operator fun get(instruction: _root_ide_package_.ir.value.LocalValue): Location {
        return creationInfo[instruction] ?:
            throw CreationInfoException("value doesn't exist: instruction=$instruction")
    }

    companion object {
        fun create(fd: FunctionData): CreationInfo {
            val creationInfo = hashMapOf<_root_ide_package_.ir.value.LocalValue, Location>()

            for (arg in fd.arguments()) {
                creationInfo[arg] = Location(fd.blocks.begin(), -1)
            }

            for (bb in fd.blocks) {
                for ((idx, instruction) in bb.iterator().withIndex()) {
                    if (instruction !is _root_ide_package_.ir.value.LocalValue) {
                        continue
                    }

                    creationInfo[instruction] = Location(bb, idx)
                }
            }

            return CreationInfo(creationInfo)
        }
    }
}