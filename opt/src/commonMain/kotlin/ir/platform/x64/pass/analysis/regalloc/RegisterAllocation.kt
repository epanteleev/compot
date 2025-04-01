package ir.platform.x64.pass.analysis.regalloc

import asm.Operand
import asm.x64.*
import ir.value.*
import ir.Definitions.QWORD_SIZE
import ir.instruction.Callable
import ir.module.MutationMarker
import ir.pass.common.AnalysisResult
import ir.platform.x64.CallConvention
import ir.platform.x64.CallConvention.gpCallerSaveRegs
import ir.platform.x64.CallConvention.xmmCallerSaveRegs
import ir.types.UndefType


class RegisterAllocation internal constructor(private val spilledLocalsStackSize: Int,
                         private val registerMap: Map<LocalValue, Operand>,
                         val calleeSaveRegisters: List<GPRegister>,
                         private val callInfo: Map<Callable, List<Operand?>>,
                         marker: MutationMarker): AnalysisResult(marker) {

    private fun frameSize(savedRegisters: Set<GPRegister>, savedXmmRegisters: Set<XmmRegister>): Int {
        return (savedRegisters.size + savedXmmRegisters.size + calleeSaveRegisters.size + /** include retaddr and rbp **/ 2) * QWORD_SIZE + spilledLocalsStackSize
    }

    fun callArguments(callable: Callable): List<Operand?> {
        return callInfo[callable]!!
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

    fun operandOrNull(value: LocalValue): Operand? = registerMap[value]

    override fun toString(): String {
        val builder = StringBuilder()
        for ((v, operand) in registerMap) {
            builder.append("$v -> $operand ")
        }

        return builder.toString()
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