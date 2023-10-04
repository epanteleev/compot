package ir.codegen.x64

import asm.x64.Mem
import asm.x64.Rbp
import ir.StackAlloc
import ir.Type
import ir.Value
import ir.ValueInstruction

data class StackFrameException(override val message: String): Exception(message)

interface StackFrame {
    fun takeSlot(value: Value): Mem

    fun returnSlot(slot: Mem)

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
    private val freeStackSlots = linkedMapOf<Int, Mem>()

    private fun withAlignment(alignment: Int, value: Long): Long {
        return ((value + (alignment * 2 - 1)) / alignment) * alignment
    }

    private fun stackSlotAlloc(value: StackAlloc): Mem {
        val typeSize = value.type().dereference().size()
        val totalSize = typeSize * value.size
        frameSize = withAlignment(totalSize.toInt(), frameSize)
        return Mem(Rbp.rbp, -frameSize, typeSize)
    }

    /** Spilled value. */
    private fun valueInstructionAlloc(value: ValueInstruction): Mem {
        val typeSize = if (value.type() != Type.U1) {
            value.type().size()
        } else {
            1
        }

        val freeSlot = freeStackSlots[typeSize]
        if (freeSlot != null) {
            freeStackSlots.remove(typeSize)
            return freeSlot
        }

        frameSize = withAlignment(typeSize, frameSize)
        return Mem(Rbp.rbp, -frameSize, typeSize)
    }

    override fun takeSlot(value: Value): Mem {
        return when (value) {
            is StackAlloc       -> stackSlotAlloc(value)
            is ValueInstruction -> valueInstructionAlloc(value)
            else -> throw StackFrameException("Cannot alloc slot for this value=$value")
        }
    }

    override fun returnSlot(slot: Mem) {
        freeStackSlots[slot.size] = slot
    }

    /** @return used stack size. Assume that given value aligned by 8 byte. */
    override fun size(): Long {
        return frameSize
    }
}