package ir.platform.x64.regalloc

import asm.x64.*
import common.assertion
import ir.global.GlobalSymbol
import ir.platform.x64.CallConvention
import ir.pass.analysis.intervals.LiveIntervals
import ir.platform.x64.CallConvention.gpCalleeSaveRegs
import ir.platform.x64.CallConvention.gpCallerSaveRegs
import ir.platform.x64.CallConvention.xmmCallerSaveRegs
import ir.value.*


class RegisterAllocation(private val spilledLocalsStackSize: Int,
                         private val registerMap: Map<LocalValue, Operand>,
                         val liveness: LiveIntervals
) {
    /** Count of callee save registers in given function. */
    val calleeSaveRegisters: Set<GPRegister> by lazy { //TODO get this from *RegisterList
        val registers = linkedSetOf<GPRegister>()
        for (reg in registerMap.values) {
            if (reg !is GPRegister) {
                continue
            }

            if (!gpCalleeSaveRegs.contains(reg)) {
                continue
            }

            registers.add(reg)
        }

        registers
    }

    private fun frameSize(savedRegisters: Set<GPRegister>, savedXmmRegisters: Set<XmmRegister>): Int {
        return (savedRegisters.size + savedXmmRegisters.size + calleeSaveRegisters.size + /** include retaddr and rbp **/ 2) * 8 + spilledLocalsStackSize
    }

    fun spilledLocalsSize(): Int = spilledLocalsStackSize

    fun callerSaveRegisters(liveOutOperands: Collection<LocalValue>, exclude: Set<LocalValue>): SavedContext {
        val registers = linkedSetOf<GPRegister>()
        val xmmRegisters = linkedSetOf<XmmRegister>()
        for (value in liveOutOperands) {
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
        return when (value) {
            is LocalValue -> {
                val operand = registerMap[value]
                assertion(operand != null) {
                    "cannot find operand for $value"
                }
                operand as Operand
            }
            is U8Value -> Imm32.of(value.u8.toLong())
            is I8Value -> Imm32.of(value.i8.toLong())
            is U16Value -> Imm32.of(value.u16.toLong())
            is I16Value -> Imm32.of(value.i16.toLong())
            is U32Value -> Imm32.of(value.u32.toLong())
            is I32Value -> Imm32.of(value.i32.toLong())
            is I64Value -> Imm64.of(value.i64)
            is U64Value -> Imm64.of(value.u64)
            is F32Value -> ImmFp32(value.f32)
            is F64Value -> ImmFp64(value.f64)
            is GlobalSymbol -> Address.internal(value.name())
            is NullValue -> Imm64.of(0)
            else -> throw RuntimeException("found '$value': '${value.type()}'")
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for ((v, operand) in registerMap) {
            builder.append("$v -> $operand ")
        }

        return builder.toString()
    }
}

data class SavedContext(val savedRegisters: Set<GPRegister>, val savedXmmRegisters: Set<XmmRegister>, private val frameSize: Int) {
    fun adjustStackSize(): Int {
        var sizeToAdjust = savedXmmRegisters.size * 8
        val remains = frameSize % CallConvention.STACK_ALIGNMENT
        if (remains != 0L) {
            sizeToAdjust += remains.toInt()
        }

        return sizeToAdjust
    }
}