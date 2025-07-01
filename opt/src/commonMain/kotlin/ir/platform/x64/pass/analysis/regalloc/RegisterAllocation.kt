package ir.platform.x64.pass.analysis.regalloc

import asm.x64.*
import ir.value.*
import ir.module.MutationMarker
import ir.pass.common.AnalysisResult


class RegisterAllocation internal constructor(private val spilledLocalsAreaSize: Int,
                                              private val registerMap: Map<LocalValue, VReg>,
                                              val calleeSaveRegisters: List<GPRegister>,
                                              marker: MutationMarker): AnalysisResult(marker) {
    fun spilledLocalsSize(): Int = spilledLocalsAreaSize

    fun calleeSaveRegs(): List<GPRegister> {
        return calleeSaveRegisters
    }

    fun vRegOrNull(value: LocalValue): VReg? = registerMap[value]

    override fun toString(): String = buildString {
        for ((v, operand) in registerMap) {
            append("$v -> $operand ")
        }
    }
}