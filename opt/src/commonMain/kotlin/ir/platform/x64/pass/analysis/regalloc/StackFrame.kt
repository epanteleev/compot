package ir.platform.x64.pass.analysis.regalloc

import ir.value.*
import ir.types.Type
import asm.x64.Address
import asm.x64.ArgumentSlot
import asm.x64.GPRegister.*
import common.assertion
import ir.Definitions.QWORD_SIZE
import ir.instruction.Generate
import ir.types.NonTrivialType


data class StackFrameException(override val message: String): Exception(message)

interface StackFrame {
    fun takeSlot(value: Value): Address
    fun returnSlot(slot: Address, size: Int)
    fun takeArgument(index: Int, arg: Value): Address
    fun size(): Int

    companion object {
        fun create(isBasePointerAddressed: Boolean = true): StackFrame {
            assertion(isBasePointerAddressed) { "unsupported other approach" }
            return BasePointerAddressedStackFrame()
        }
    }
}

private class BasePointerAddressedStackFrame : StackFrame {
    private var frameSize: Int = 0
    private val freeStackSlots = linkedMapOf<Int, Address>()

    private fun getTypeSize(ty: NonTrivialType): Int {
        return if (ty != Type.U1) {
            ty.sizeOf()
        } else {
            1
        }
    }

    private fun withAlignment(alignment: Int, value: Int): Int {
        return ((value + (alignment * 2 - 1)) / alignment) * alignment
    }

    private fun stackSlotAlloc(value: Generate): Address {
        val typeSize = getTypeSize(value.type())
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
            is Generate   -> stackSlotAlloc(value)
            is LocalValue -> valueInstructionAlloc(value)
            else -> throw StackFrameException("Cannot alloc slot for this value=$value")
        }
    }

    override fun returnSlot(slot: Address, size: Int) {
        if (freeStackSlots.containsKey(size)) {
            return
        }

        freeStackSlots[size] = slot
    }

    override fun size(): Int {
        return frameSize
    }

    override fun takeArgument(index: Int, arg: Value): Address {
        frameSize = withAlignment(QWORD_SIZE, frameSize)
        return ArgumentSlot(rsp, index * QWORD_SIZE)
    }
}