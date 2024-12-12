package ir.platform.x64.pass.analysis.regalloc

import asm.x64.*
import ir.types.*
import asm.Operand
import asm.x64.GPRegister.rbp
import ir.value.ArgumentValue
import ir.Definitions.QWORD_SIZE
import ir.Definitions.DOUBLE_SIZE
import ir.Definitions.alignTo
import ir.platform.x64.CallConvention


internal class CalleeArgumentAllocator private constructor() {
    private var gpRegPos = 0
    private var xmmRegPos = 0
    private var argumentSlotIndex: Int = 0

    private fun peakIntegerArgument(): Operand {
        if (gpRegPos < gpRegisters.size) {
            gpRegPos += 1
            return gpRegisters[gpRegPos - 1]
        } else {
            val old = argumentSlotIndex
            argumentSlotIndex += 1
            return ArgumentSlot(rbp, old * QWORD_SIZE + OVERFLOW_AREA_OFFSET)
        }
    }

    private fun peakFPArgument(): Operand {
        if (xmmRegPos < fpRegisters.size) {
            xmmRegPos += 1
            return fpRegisters[xmmRegPos - 1]
        } else {
            val old = argumentSlotIndex
            argumentSlotIndex += 1
            return ArgumentSlot(rbp, old * DOUBLE_SIZE + OVERFLOW_AREA_OFFSET)
        }
    }

    private fun peakStructArgument(structType: StructType): Operand {
        val size = structType.sizeOf()
        val old = argumentSlotIndex
        val offset = alignTo(argumentSlotIndex * QWORD_SIZE + OVERFLOW_AREA_OFFSET + size, QWORD_SIZE)
        argumentSlotIndex = argumentSlotIndex + offset / QWORD_SIZE
        return ArgumentSlot(rbp, old * DOUBLE_SIZE + OVERFLOW_AREA_OFFSET)
    }

    private fun pickArgument(type: NonTrivialType): Operand = when (type) {
        is IntegerType, is PtrType -> peakIntegerArgument()
        is FloatingPointType           -> peakFPArgument()
        is StructType                  -> peakStructArgument(type)
        else -> throw IllegalArgumentException("not allowed for this type=$type")
    }

    private fun allocate(arguments: List<ArgumentValue>): List<Operand> {
        return arguments.mapTo(arrayListOf()) {
            pickArgument(it.contentType())
        }
    }

    companion object {
        private const val RETURN_ADDRESS_SIZE = QWORD_SIZE
        private const val FRAME_POINTER_SIZE = QWORD_SIZE
        const val OVERFLOW_AREA_OFFSET = RETURN_ADDRESS_SIZE + FRAME_POINTER_SIZE

        private val gpRegisters = CallConvention.gpArgumentRegisters
        private val fpRegisters = CallConvention.xmmArgumentRegister

        fun allocate(arguments: List<ArgumentValue>): List<Operand> {
            return CalleeArgumentAllocator().allocate(arguments)
        }
    }
}