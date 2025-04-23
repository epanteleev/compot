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
import ir.types.AggregateType
import ir.types.UndefType


class RegisterAllocation internal constructor(private val spilledLocalsStackSize: Int,
                                              private val registerMap: Map<LocalValue, VReg>,
                                              val calleeSaveRegisters: List<GPRegister>,
                                              private val overflowAreaSize: Map<Callable, Int>,
                                              marker: MutationMarker): AnalysisResult(marker) {

    private fun frameSize(savedRegisters: Set<GPRegister>, savedXmmRegisters: Set<XmmRegister>): Int {
        return (savedRegisters.size + savedXmmRegisters.size + calleeSaveRegisters.size + /** include retaddr and rbp **/ 2) * QWORD_SIZE + spilledLocalsStackSize
    }

    fun spilledLocalsSize(): Int = spilledLocalsStackSize

    fun callerSaveRegisters(liveOutOperands: Collection<LocalValue>, exclude: Set<LocalValue>): SavedContext {
        val registers = hashSetOf<GPRegister>()
        val xmmRegisters = hashSetOf<XmmRegister>()
        for (value in liveOutOperands) {
            if (value.type() == UndefType) {
                continue
            }

            if (exclude.contains(value)) {
                continue
            }

            when (val reg = registerMap[value]!!) {
                is GPRegister -> {
                    if (!gpCallerSaveRegs.contains(reg)) {
                        continue
                    }

                    registers.add(reg)
                }
                is XmmRegister -> {
                    if (!xmmCallerSaveRegs.contains(reg)) {
                        continue
                    }

                    xmmRegisters.add(reg)
                }
                else -> {}
            }
        }

        return SavedContext(registers, xmmRegisters, frameSize(registers, xmmRegisters))
    }

    fun vRegOrNull(value: LocalValue): VReg? = registerMap[value]

    fun overflowAreaSize(call: Callable): Int =
        overflowAreaSize[call] ?: throw IllegalArgumentException("call=$call not found in overflowAreaSize")

    override fun toString(): String = buildString {
        for ((v, operand) in registerMap) {
            append("$v -> $operand ")
        }
    }
}

class SavedContext internal constructor(val savedGPRegisters: Set<GPRegister>, val savedXmmRegisters: Set<XmmRegister>, private val frameSize: Int) {
    fun adjustStackSize(overflowAreaSize: Int): Int {
        var sizeToAdjust = savedXmmRegisters.size * QWORD_SIZE + overflowAreaSize
        val remains = (frameSize + overflowAreaSize) % CallConvention.STACK_ALIGNMENT
        if (remains != 0L) {
            sizeToAdjust += remains.toInt()
        }

        return sizeToAdjust
    }
}