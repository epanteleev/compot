package ir.codegen.x64

import asm.x64.*
import ir.*
import java.lang.IllegalArgumentException

class VirtualRegistersPool {
    private val frame = StackFrame.create()
    private val gpRegisters = GPRegistersList()

    /** Arguments matching. */
    private var freeArgumentRegisters = CallConvention.gpArgumentRegisters.toMutableList().asReversed()
    private var argumentSlotIndex: Long = 0
    private val allocatedArgumentStackSlots = arrayListOf<ArgumentSlot>()

    private fun pickArgument(type: Type): Operand {
        assert(type.isSigned() || type.isUnsigned() || type.isPointer())

        val slot = if (freeArgumentRegisters.isNotEmpty()) {
            freeArgumentRegisters.removeLast()
        } else {

            val old = argumentSlotIndex
            argumentSlotIndex += 1
            val slot = ArgumentSlot(Rbp.rbp, old /*actual value is frameBase + 8L * old*/, 8)
            allocatedArgumentStackSlots.add(slot)
            slot
        }

        return slot
    }

    fun allocSlot(value: Value): Operand {
        when (value) {
            is ArgumentValue -> {
                return pickArgument(value.type())
            }

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

    fun finalize() {
        val addressSize = 8L
        val calleeSaveRegs = gpRegisters.usedCalleeSaveRegisters()
        for (slot in allocatedArgumentStackSlots) {
            slot.updateOffset(calleeSaveRegs.size * 8L + 8L * slot.offset + addressSize)
        }
        allocatedArgumentStackSlots.clear()
    }

    fun stackSize(): Long {
        return frame.size()
    }
}