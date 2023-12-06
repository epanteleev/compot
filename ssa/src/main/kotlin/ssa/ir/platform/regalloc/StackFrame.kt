package ir.platform.regalloc

import asm.x64.*
import ir.instruction.Alloc
import ir.Value
import ir.instruction.ValueInstruction
import ir.types.Type

data class StackFrameException(override val message: String): Exception(message)

interface StackFrame {
    fun takeSlot(value: Value): Address
    fun returnSlot(slot: Address)
    fun size(): Long

    companion object {
        fun create(isBasePointerAddressed: Boolean = true): StackFrame {
            assert(isBasePointerAddressed) { "unsupported other approach" }
            return BasePointerAddressedStackFrame()
        }
    }
}

private class BasePointerAddressedStackFrame : StackFrame {
    private var frameSize: Long = 0
    private val freeStackSlots = linkedMapOf<Int, Address>()

    private fun getTypeSize(ty: Type): Int {
        return if (ty != Type.U1) {
            ty.size()
        } else {
            1
        }
    }

    private fun withAlignment(alignment: Int, value: Long): Long {
        return ((value + (alignment * 2 - 1)) / alignment) * alignment
    }

    private fun stackSlotAlloc(value: Alloc): Address {
        val typeSize = getTypeSize(value.allocatedType)

        frameSize = withAlignment(typeSize, frameSize)
        return Address.mem(Rbp.rbp, -frameSize, typeSize)
    }

    /** Spilled value. */
    private fun valueInstructionAlloc(value: ValueInstruction): Address {
        val typeSize = getTypeSize(value.type())

        val freeSlot = freeStackSlots[typeSize]
        if (freeSlot != null) {
            freeStackSlots.remove(typeSize)
            return freeSlot
        }

        frameSize = withAlignment(typeSize, frameSize)
        return Address.mem(Rbp.rbp, -frameSize, typeSize)
    }

    override fun takeSlot(value: Value): Address {
        return when (value) {
            is Alloc -> stackSlotAlloc(value)
            is ValueInstruction -> valueInstructionAlloc(value)
            else -> throw StackFrameException("Cannot alloc slot for this value=$value")
        }
    }

    override fun returnSlot(slot: Address) {
        freeStackSlots[slot.size] = slot
    }

    override fun size(): Long {
        return frameSize
    }
}