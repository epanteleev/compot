package ir.codegen.x64.regalloc

import asm.x64.*
import ir.*
import ir.codegen.x64.CallConvention
import ir.instruction.StackAlloc
import ir.instruction.ValueInstruction
import java.lang.IllegalArgumentException

class VirtualRegistersPool {
    private val frame = StackFrame.create()
    private val gpRegisters = GPRegistersList()

    /** Arguments matching. */
    private var freeArgumentRegisters = CallConvention.gpArgumentRegisters.toMutableList().asReversed()
    private var argumentSlotIndex: Long = 0

    private fun pickArgument(type: Type): Operand {
        assert(type.isSigned() || type.isUnsigned() || type.isPointer())

        val slot = if (freeArgumentRegisters.isNotEmpty()) {
            if (type == Type.U1) {
                freeArgumentRegisters.removeLast().invoke(1)
            } else {
                freeArgumentRegisters.removeLast().invoke(type.size())
            }
        } else {

            val old = argumentSlotIndex
            argumentSlotIndex += 1
            val slotSize = if (type == Type.U1) {
                1
            } else {
                type.size()
            }
            ArgumentSlot(Rbp.rbp, old * 8 + 16, slotSize)
        }

        return slot
    }

    fun allocArgument(value: ArgumentValue): Operand {
        return pickArgument(value.type())
    }

    fun allocSlot(value: Value): Operand {
        when (value) {
            is StackAlloc -> {
                return frame.takeSlot(value)
            }

            is ValueInstruction -> {
                val registerOrNull = gpRegisters.pickRegister(value)
                if (registerOrNull != null) {
                    return registerOrNull
                }

                return frame.takeSlot(value)
            }

            else -> {
                throw IllegalArgumentException("not allowed for this value=$value")
            }
        }
    }

    fun free(operand: Operand) {
        when (operand) {
            is GPRegister -> gpRegisters.returnRegister(operand)
            is Mem        -> frame.returnSlot(operand)
            else          -> TODO("Unimplemented")
        }
    }

    fun stackSize(): Long {
        return ((frame.size() + 7) / 8) * 8
    }
}