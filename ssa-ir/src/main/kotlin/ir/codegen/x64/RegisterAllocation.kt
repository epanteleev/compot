package ir.codegen.x64

import asm.x64.*
import ir.*
import ir.codegen.x64.regalloc.liveness.LiveIntervals
import ir.instruction.ValueInstruction
import ir.utils.OrderedLocation
import java.lang.StringBuilder

class RegisterAllocation(private var stackSize: Long,
                         private val registerMap: MutableMap<LocalValue, Operand>,
                         private val liveness: LiveIntervals
) {
    /** Count of callee save registers in given function. */
    val calleeSaveRegisters: Set<GPRegister> by lazy { calleeSaveRegistersInternal() }

    fun liveness(): LiveIntervals {
        return liveness
    }

    fun reservedStackSize(): Long {
        return stackSize
    }

    private fun calleeSaveRegistersInternal(): Set<GPRegister> {
        val registers = linkedSetOf<GPRegister>()
        for (reg in registerMap.values) {
            if (reg !is GPRegister) {
                continue
            }
            val reg8 = reg(8)
            if (CallConvention.gpCalleeSaveRegs.contains(reg8)) {
                if (registers.contains(reg8)) { //Todo
                    continue
                }
                registers.add(reg8)
            }
        }

        return registers
    }

    /** Get used caller save registers in given location. */
    fun callerSaveRegisters(loc: OrderedLocation): Set<GPRegister> {
        val registers = linkedSetOf<GPRegister>()
        for ((value, reg) in registerMap) {
            if (reg !is GPRegister) {
                continue
            }
            val reg8 = reg(8)
            if (!CallConvention.gpCallerSaveRegs.contains(reg8)) {
                continue
            }
            if (liveness[value].end() > loc) {
                registers.add(reg8)
            }
        }

        return registers
    }

    fun operand(value: Value): AnyOperand {
        return when (value) {
            is ValueInstruction, is ArgumentValue -> registerMap[value] as Operand
            is U8Value  -> Imm(value.u8.toLong(), 1)
            is I8Value  -> Imm(value.i8.toLong(), 1)
            is U16Value -> Imm(value.u16.toLong(), 2)
            is I16Value -> Imm(value.i16.toLong(), 2)
            is U32Value -> Imm(value.u32.toLong(), 4)
            is I32Value -> Imm(value.i32.toLong(), 4)
            is I64Value -> Imm(value.i64, 8)
            is U64Value -> Imm(value.u64, 8)
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