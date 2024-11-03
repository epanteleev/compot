package ir.platform.x64.pass.analysis.regalloc

import asm.Operand
import asm.x64.*
import ir.types.*
import asm.x64.GPRegister.rbp
import ir.value.ArgumentValue
import ir.Definitions.QWORD_SIZE
import ir.Definitions.DOUBLE_SIZE
import ir.platform.x64.CallConvention


internal class CallerArgumentAllocator private constructor() {
    private var freeArgumentRegisters = CallConvention.gpArgumentRegisters.toMutableList().asReversed()
    private var freeXmmArgumentRegisters = CallConvention.xmmArgumentRegister.toMutableList().asReversed()
    private var argumentSlotIndex: Int = 0

    private fun peakIntegerArgument(): Operand {
        return if (freeArgumentRegisters.isNotEmpty()) {
            freeArgumentRegisters.removeLast()
        } else {
            val old = argumentSlotIndex
            argumentSlotIndex += 1
            ArgumentSlot(rbp, old * QWORD_SIZE + ARGUMENTS_OFFSET)
        }
    }

    private fun peakFPArgument(): Operand {
        return if (freeXmmArgumentRegisters.isNotEmpty()) {
            freeXmmArgumentRegisters.removeLast()
        } else {
            val old = argumentSlotIndex
            argumentSlotIndex += 1
            ArgumentSlot(rbp, old * DOUBLE_SIZE + ARGUMENTS_OFFSET)
        }
    }

    private fun peakStructArgument(type: AggregateType): Operand {
        val size = type.sizeOf()
        val old = argumentSlotIndex
        argumentSlotIndex += (size + QWORD_SIZE - 1) / QWORD_SIZE
        return ArgumentSlot(rbp, old * QWORD_SIZE + ARGUMENTS_OFFSET)
    }

    private fun pickArgument(type: NonTrivialType): Operand = when (type) {
        is IntegerType, is PointerType -> peakIntegerArgument()
        is FloatingPointType -> peakFPArgument()
        is AggregateType -> peakStructArgument(type)
        else -> throw IllegalArgumentException("not allowed for this type=$type")
    }

    fun allocate(arguments: List<ArgumentValue>): List<Operand> {
        return arguments.mapTo(arrayListOf()) {
            pickArgument(it.contentType())
        }
    }

    companion object {
        private const val RETURN_ADDRESS_SIZE = QWORD_SIZE
        private const val FRAME_POINTER_SIZE = QWORD_SIZE
        const val ARGUMENTS_OFFSET = RETURN_ADDRESS_SIZE + FRAME_POINTER_SIZE

        fun allocate(arguments: List<ArgumentValue>): List<Operand> {
            return CallerArgumentAllocator().allocate(arguments)
        }
    }
}