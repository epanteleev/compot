package ir.platform.x64.regalloc

import ir.*
import asm.x64.*
import ir.utils.OrderedLocation
import ir.platform.x64.CallConvention
import ir.platform.x64.regalloc.liveness.LiveIntervals
import ir.platform.x64.CallConvention.gpCalleeSaveRegs


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

            if (!reg.isCallEESave) {
                continue
            }
            assert(gpCalleeSaveRegs.contains(reg)) //TODO

            registers.add(reg)
        }

        registers
    }

    fun frameSize(savedRegisters: Set<GPRegister>): Int {
        return (savedRegisters.size + calleeSaveRegisters.size + /** include retaddr and rbp **/ 2) * 8 + reservedStackSize()
    }

    fun reservedStackSize(): Int {
        return stackSize
    }

    /** Get used caller save registers in given location. */
    fun callerSaveRegisters(loc: OrderedLocation): Set<GPRegister> {
        val registers = linkedSetOf<GPRegister>()
        for ((value, reg) in registerMap) {
            if (reg !is GPRegister) {
                continue
            }
            if (!reg.isCallERSave) {
                continue
            }

            assert(CallConvention.gpCallerSaveRegs.contains(reg))

            val liveRange = liveness[value]
            if (liveRange.end() > loc && loc > liveRange.begin()) {
                registers.add(reg)
            }
        }

        return registers
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