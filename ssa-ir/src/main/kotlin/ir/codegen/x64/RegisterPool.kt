package ir.codegen.x64

import asm.x64.*
import ir.ArgumentValue
import ir.StackAlloc
import ir.Type
import ir.Value

class RegisterPool {
    private var totalFrameSize: Long = 0
    private var freeRegisters = CallConvention.availableRegisters.toMutableList().asReversed()
    private val freeStackSlots = linkedMapOf<Int, Mem>()
    private val calleeSaveRegistersCount = mutableSetOf<GPRegister>()

    /** Arguments matching. */
    private var freeArgumentRegisters = CallConvention.gpArgumentRegisters.toMutableList().asReversed()
    private var slotIndex: Long = 0
    private val allocatedArgumentStackSlots = arrayListOf<ArgumentSlot>()

    private fun pickArgument(type: Type): Operand {
        assert(type.isSigned() || type.isUnsigned() || type.isPointer())

        val slot = if (freeArgumentRegisters.isNotEmpty()) {
            freeArgumentRegisters.removeLast()
        } else {
            calleeSaveRegistersCount.add(Rbp.rbp)

            val old = slotIndex
            slotIndex += 1
            val slot = ArgumentSlot(Rbp.rbp, old /*actual value is frameBase + 8L * old*/, 8)
            allocatedArgumentStackSlots.add(slot)
            slot
        }

        return slot
    }

    private fun stackSlotAlloc(value: StackAlloc): Mem {
        val typeSize = value.type().size()
        val totalSize = typeSize * value.size
        totalFrameSize += totalSize
        calleeSaveRegistersCount.add(Rbp.rbp)
        return Mem(Rbp.rbp, -totalFrameSize, typeSize)
    }

    private fun tryPickRegister(value: Value): Register? {
        assert(value.type() == Type.U1 ||
                value.type().isSigned() ||
                value.type().isUnsigned() ||
                value.type().isPointer()) {
            "found ${value.type()}"
        }

        if (freeRegisters.isEmpty()) {
            return null
        }
        val reg = freeRegisters.removeLast()
        if (CallConvention.gpNonVolatileRegs.contains(reg)) {
            calleeSaveRegistersCount.add(reg)
        }
        return reg
    }

    private fun spillValue(value: Value): Mem {
        val typeSize = value.type().size()
        val freeSlot = freeStackSlots[typeSize]
        if (freeSlot != null) {
            freeStackSlots.remove(typeSize)
            return freeSlot
        }

        calleeSaveRegistersCount.add(Rbp.rbp)
        totalFrameSize += typeSize
        return Mem(Rbp.rbp, -totalFrameSize, typeSize)
    }

    fun allocSlot(value: Value): Operand {
        if (value is ArgumentValue) {
            return pickArgument(value.type())
        }
        if (value is StackAlloc) {
            return stackSlotAlloc(value)
        }

        val registerOrNull = tryPickRegister(value)
        if (registerOrNull != null) {
            return registerOrNull
        }

        return spillValue(value)
    }

    fun free(operand: Operand) {
        when (operand) {
            is GPRegister -> freeRegisters.add(operand)
            is Mem -> freeStackSlots[operand.size] = operand
            else -> TODO("Unimplemented")
        }
    }

    fun finalize() {
        val addressSize = 8L
        for (slot in allocatedArgumentStackSlots) {
            slot.updateOffset(calleeSaveRegistersCount.size * 8L + 8L * slot.offset + addressSize)
        }
        allocatedArgumentStackSlots.clear()
    }

    fun stackSize(): Long {
        return totalFrameSize
    }
}