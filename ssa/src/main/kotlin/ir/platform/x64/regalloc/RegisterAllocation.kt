package ir.platform.x64.regalloc

import ir.*
import asm.x64.*
import ir.utils.OrderedLocation
import ir.platform.x64.CallConvention
import ir.platform.x64.regalloc.liveness.LiveIntervals
import ir.platform.x64.CallConvention.gpCalleeSaveRegs
import ir.platform.x64.CallConvention.gpCallerSaveRegs
import ir.platform.x64.CallConvention.xmmCallerSaveRegs


class RegisterAllocation(private val stackSize: Int,
                         private val registerMap: Map<LocalValue, Operand>,
                         private val liveness: LiveIntervals
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
        return (savedRegisters.size + savedXmmRegisters.size + calleeSaveRegisters.size + /** include retaddr and rbp **/ 2) * 8 + stackSize
    }

    fun spilledLocalsSize(): Int = stackSize

    /** Get used caller save registers in given location. */
    fun callerSaveRegisters(loc: OrderedLocation): SavedContext {
        val registers = linkedSetOf<GPRegister>()
        val xmmRegisters = linkedSetOf<XmmRegister>()
        for ((value, reg) in registerMap) {
            when (reg) {
                is GPRegister -> {
                    if (!gpCallerSaveRegs.contains(reg)) {
                        continue
                    }

                    val liveRange = liveness[value]
                    if (liveRange.end() > loc && loc > liveRange.begin()) {
                        registers.add(reg)
                    }
                }
                is XmmRegister -> {
                    if (!xmmCallerSaveRegs.contains(reg)) {
                        continue
                    }

                    val liveRange = liveness[value]
                    if (liveRange.end() > loc && loc > liveRange.begin()) {
                        xmmRegisters.add(reg)
                    }
                }
            }
        }

        return SavedContext(registers, xmmRegisters, frameSize(registers, xmmRegisters))
    }

    fun operand(value: Value): Operand {
        return when (value) {
            is LocalValue -> {
                val operand = registerMap[value]
                assert(operand != null) {
                    "cannot find operand for $value"
                }
                operand as Operand
            }
            is U8Value     -> Imm32(value.u8.toLong())
            is I8Value     -> Imm32(value.i8.toLong())
            is U16Value    -> Imm32(value.u16.toLong())
            is I16Value    -> Imm32(value.i16.toLong())
            is U32Value    -> Imm32(value.u32.toLong())
            is I32Value    -> Imm32(value.i32.toLong())
            is I64Value    -> Imm64(value.i64)
            is U64Value    -> Imm64(value.u64)
            is F32Value    -> ImmFp32(value.f32)
            is F64Value    -> ImmFp64(value.f64)
            is GlobalSymbol -> Address.from(value.name())
            else -> throw RuntimeException("expect $value: ${value.type()}")
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