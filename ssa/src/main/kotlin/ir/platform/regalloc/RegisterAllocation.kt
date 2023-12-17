package ir.platform.regalloc

import asm.x64.*
import ir.*
import ir.platform.liveness.LiveIntervals
import ir.platform.x64.CallConvention
import ir.platform.x64.CallConvention.gpCalleeSaveRegs
import ir.utils.OrderedLocation

class RegisterAllocation(private val stackSize: Long,
                         private val registerMap: Map<LocalValue, Operand>,
                         private val liveness: LiveIntervals
) {
    /** Count of callee save registers in given function. */
    val calleeSaveRegisters: Set<GPRegister> by lazy {
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

    fun frameSize(savedRegisters: Set<GPRegister>): Long {
        return (savedRegisters.size + calleeSaveRegisters.size + /** include retaddr and rbp **/ 2) * 8 + reservedStackSize()
    }

    fun reservedStackSize(): Long {
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

    fun operand(value: Value): AnyOperand {
        return when (value) {
            is LocalValue  -> {
                val operand = registerMap[value]
                assert(operand != null) {
                    "cannot find operand for $value"
                }
                operand as Operand
            }
            is U8Value     -> Imm(value.u8.toLong())
            is I8Value     -> Imm(value.i8.toLong())
            is U16Value    -> Imm(value.u16.toLong())
            is I16Value    -> Imm(value.i16.toLong())
            is U32Value    -> Imm(value.u32.toLong())
            is I32Value    -> Imm(value.i32.toLong())
            is I64Value    -> Imm(value.i64)
            is U64Value    -> Imm(value.u64)
            is GlobalValue -> Address.mem(value.name())
            else -> throw RuntimeException("expect $value:${value.type()}")
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