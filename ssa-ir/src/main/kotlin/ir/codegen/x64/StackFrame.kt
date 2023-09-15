package ir.codegen.x64

import asm.x64.Mem
import asm.x64.Rbp
import ir.StackAlloc
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
    private var totalFrameSize: Long = 0
    private val freeStackSlots = linkedMapOf<Int, Mem>()

    private fun stackSlotAlloc(value: StackAlloc): Mem {
        val typeSize = value.type().size()
        val totalSize = typeSize * value.size
        totalFrameSize += totalSize
        return Mem(Rbp.rbp, -totalFrameSize, typeSize)
    }

    /** Spilled value. */
    private fun valueInstructionAlloc(value: ValueInstruction): Mem {
        val typeSize = value.type().size()
        val freeSlot = freeStackSlots[typeSize]
        if (freeSlot != null) {
            freeStackSlots.remove(typeSize)
            return freeSlot
        }

        totalFrameSize += typeSize
        return Mem(Rbp.rbp, -totalFrameSize, typeSize)
    }

    override fun takeSlot(value: Value): Mem {
        return when (value) {
            is StackAlloc -> {
                stackSlotAlloc(value)
            }

            is ValueInstruction -> {
                valueInstructionAlloc(value)
            }

            else -> {
                throw StackFrameException("Cannot alloc slot for this value=$value")
            }
        }
    }

    override fun returnSlot(slot: Mem) {
        freeStackSlots[slot.size] = slot
    }

    override fun size(): Long {
        return totalFrameSize
    }
}