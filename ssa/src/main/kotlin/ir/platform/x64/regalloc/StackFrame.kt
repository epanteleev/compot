package ir.platform.x64.regalloc

import ir.Value
import ir.types.Type
import asm.x64.Address
import asm.x64.GPRegister.*
import ir.LocalValue
import ir.asType
import ir.instruction.Alloc
import ir.instruction.ValueInstruction
import ir.types.NonTrivialType


data class StackFrameException(override val message: String): Exception(message)

interface StackFrame {
    fun takeSlot(value: Value): Address
    fun returnSlot(slot: Address, size: Int)
    fun size(): Int

    companion object {
        fun create(isBasePointerAddressed: Boolean = true): StackFrame {
            assert(isBasePointerAddressed) { "unsupported other approach" }
            return BasePointerAddressedStackFrame()
        }
    }
}

private class BasePointerAddressedStackFrame : StackFrame {
    private var frameSize: Int = 0
    private val freeStackSlots = linkedMapOf<Int, Address>()

    private fun getTypeSize(ty: NonTrivialType): Int {
        return if (ty != Type.U1) {
            ty.size()
        } else {
            1
        }
    }

    private fun withAlignment(alignment: Int, value: Int): Int {
        return ((value + (alignment * 2 - 1)) / alignment) * alignment
    }

    private fun stackSlotAlloc(value: Alloc): Address {
        val typeSize = getTypeSize(value.allocatedType)

        frameSize = withAlignment(typeSize, frameSize)
        return Address.from(rbp, -frameSize)
    }

    /** Spilled value. */
    private fun valueInstructionAlloc(value: LocalValue): Address {
        val typeSize = getTypeSize(value.asType())

        val freeSlot = freeStackSlots[typeSize]
        if (freeSlot != null) {
            freeStackSlots.remove(typeSize)
            return freeSlot
        }

        frameSize = withAlignment(typeSize, frameSize)
        return Address.from(rbp, -frameSize)
    }

    override fun takeSlot(value: Value): Address {
        return when (value) {
            is Alloc            -> stackSlotAlloc(value)
            is LocalValue -> valueInstructionAlloc(value)
            else -> throw StackFrameException("Cannot alloc slot for this value=$value")
        }
    }

    override fun returnSlot(slot: Address, size: Int) {
        freeStackSlots[size] = slot
    }

    override fun size(): Int {
        return frameSize
    }
}