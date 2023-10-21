package ir.platform.regalloc

import ir.*
import asm.x64.*
import java.lang.StringBuilder
import ir.utils.OrderedLocation
import ir.platform.x64.CallConvention
import ir.platform.x64.CallConvention.gpCalleeSaveRegs
import ir.platform.liveness.LiveIntervals

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

            val reg8 = reg(8)
            if (!reg8.isCallEESave) {
                continue
            }
            assert(gpCalleeSaveRegs.contains(reg8))

            registers.add(reg8)
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

            val reg8 = reg(8)
            assert(CallConvention.gpCallerSaveRegs.contains(reg8))

            val liveRange = liveness[value]
            if (liveRange.end() > loc && loc > liveRange.begin()) {
                registers.add(reg8)
            }
        }

        return registers
    }

    fun operand(value: Value): AnyOperand {
        return when (value) {
            is LocalValue ->  {
                val operand = registerMap[value]
                assert(operand != null) {
                    "cannot find operand for $value"
                }
                operand as Operand
            }
            is U8Value    -> Imm(value.u8.toLong(), 1)
            is I8Value    -> Imm(value.i8.toLong(), 1)
            is U16Value   -> Imm(value.u16.toLong(), 2)
            is I16Value   -> Imm(value.i16.toLong(), 2)
            is U32Value   -> Imm(value.u32.toLong(), 4)
            is I32Value   -> Imm(value.i32.toLong(), 4)
            is I64Value   -> Imm(value.i64, 8)
            is U64Value   -> Imm(value.u64, 8)
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