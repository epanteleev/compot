package ir.platform.x64.pass.analysis.regalloc

import ir.value.*
import asm.x64.Address
import asm.x64.ArgumentSlot
import asm.x64.GPRegister.*
import common.assertion
import ir.Definitions
import ir.instruction.lir.Generate
import ir.types.NonTrivialType


sealed interface StackFrame {
    fun takeSlot(value: LocalValue): Address
    fun returnSlot(slot: Address, size: Int)
    fun takeArgument(offset: Int): Address
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
    private val freeStackSlots = linkedMapOf<Int, Address>() // TODO this feature is disabled

    private fun withAlignment(alignment: Int, value: Int): Int {
        if (alignment == 0) {
            return value
        }

        return Definitions.alignTo(value, alignment)
    }

    private fun stackSlotAlloc(value: Generate): Address {
        val ty = value.type()
        frameSize = withAlignment(ty.alignmentOf(), frameSize + ty.sizeOf())
        return Address.from(rbp, -frameSize)
    }

    /** Spilled value. */
    private fun valueInstructionAlloc(value: LocalValue): Address {
        val ty = value.asType<NonTrivialType>()
        frameSize = withAlignment(ty.alignmentOf(), frameSize + ty.sizeOf())
        return Address.from(rbp, -frameSize)
    }

    override fun takeSlot(value: LocalValue): Address = when (value) {
        is Generate -> stackSlotAlloc(value)
        else -> valueInstructionAlloc(value)
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

    override fun takeArgument(offset: Int): Address {
        return ArgumentSlot(rsp, offset)
    }
}