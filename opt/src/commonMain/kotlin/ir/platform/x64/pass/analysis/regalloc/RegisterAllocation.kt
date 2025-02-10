package ir.platform.x64.pass.analysis.regalloc

import asm.Operand
import asm.x64.*
import ir.value.*
import ir.Definitions.QWORD_SIZE
import ir.global.ExternValue
import ir.global.GlobalConstant
import ir.global.GlobalValue
import ir.instruction.Callable
import ir.module.ExternFunction
import ir.module.FunctionPrototype
import ir.module.MutationMarker
import ir.pass.common.AnalysisResult
import ir.platform.x64.CallConvention
import ir.platform.x64.CallConvention.gpCallerSaveRegs
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmCallerSaveRegs
import ir.types.UndefType
import ir.value.constant.*


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

    fun operand(value: Value): Operand {
        if (value.type() == UndefType) {
            return temp1
        }

        return operandOrNull(value) ?: throw IllegalArgumentException("cannot find operand for $value")
    }

    fun operandOrNull(value: Value): Operand? = when (value) {
        is LocalValue -> registerMap[value]
        is U8Value  -> Imm32.of(value.u8.toLong())
        is I8Value  -> Imm32.of(value.i8.toLong())
        is U16Value -> Imm32.of(value.u16.toLong())
        is I16Value -> Imm32.of(value.i16.toLong())
        is U32Value -> Imm32.of(value.u32.toLong())
        is I32Value -> Imm32.of(value.i32.toLong())
        is I64Value -> Imm64.of(value.i64)
        is U64Value -> Imm64.of(value.u64)
        is GlobalConstant    -> Address.internal(value.name())
        is FunctionPrototype -> Address.internal(value.name())
        is ExternFunction    -> Address.external(value.name())
        is ExternValue -> Address.external(value.name())
        is GlobalValue -> Address.internal(value.name())
        is NullValue   -> Imm64.of(0)
        else -> null
    }

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