package ir.platform.x64.pass.analysis.regalloc

import asm.x64.*
import common.assertion
import ir.Definitions
import ir.Definitions.POINTER_SIZE
import ir.value.*
import ir.Definitions.QWORD_SIZE
import ir.instruction.Callable
import ir.module.MutationMarker
import ir.pass.common.AnalysisResult
import ir.platform.x64.CallConvention
import ir.platform.x64.CallConvention.gpCallerSaveRegs
import ir.platform.x64.CallConvention.xmmCallerSaveRegs
import ir.platform.x64.codegen.CodegenException
import ir.platform.x64.pass.analysis.callinfo.SavedContext
import ir.types.AggregateType
import ir.types.UndefType


class RegisterAllocation internal constructor(private val spilledLocalsStackSize: Int,
                                              private val registerMap: Map<LocalValue, VReg>,
                                              val calleeSaveRegisters: List<GPRegister>,
                                              marker: MutationMarker): AnalysisResult(marker) {
    fun spilledLocalsSize(): Int = spilledLocalsStackSize

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