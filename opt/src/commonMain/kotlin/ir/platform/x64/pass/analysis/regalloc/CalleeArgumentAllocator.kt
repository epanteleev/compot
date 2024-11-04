package ir.platform.x64.pass.analysis.regalloc

import asm.Operand
import common.assertion
import ir.value.Value
import ir.Definitions.QWORD_SIZE
import ir.instruction.Generate
import ir.types.*
import ir.platform.x64.CallConvention


class CalleeArgumentAllocator private constructor(private val stackFrame: StackFrame, private val arguments: List<Value>) {
    private sealed interface Place
    private data class Memory(val index: Int, val size: Int): Place
    private data class RealGPRegister(val registerIndex: Int): Place
    private data class RealFpRegister(val registerIndex: Int): Place

    private var gpRegPos = 0
    private var xmmRegPos = 0
    private var memSlots = 0

    private fun emit(value: Value): Place? = when (value.type()) {
        is FloatingPointType -> {
            if (xmmRegPos < fpRegisters.size) {
                xmmRegPos += 1
                RealFpRegister(xmmRegPos - 1)
            } else {
                memSlots += 1
                Memory(memSlots - 1, (memSlots - 1) * QWORD_SIZE)
            }
        }
        is IntegerType, is PointerType, is FlagType -> {
            if (gpRegPos < gpRegisters.size) {
                gpRegPos += 1
                RealGPRegister(gpRegPos - 1)
            } else {
                memSlots += 1
                Memory(memSlots - 1, (memSlots - 1) * QWORD_SIZE)
            }
        }
        is AggregateType -> {
            assertion(value is Generate) { "value=$value" }
            value as Generate
            val size = value.type().sizeOf()
            val slot = memSlots
            memSlots += (size + QWORD_SIZE - 1) / QWORD_SIZE //TODO
            Memory(slot, (memSlots - 1) * QWORD_SIZE)
        }
        is UndefType -> null
        else -> throw IllegalArgumentException("type=$value")
    }


    private fun calculate(): List<Operand?> {
        val allocation = arrayListOf<Operand?>()
        for (arg in arguments) {
            val operand = when (val pos = emit(arg)) {
                is RealGPRegister -> gpRegisters[pos.registerIndex]
                is RealFpRegister -> fpRegisters[pos.registerIndex]
                is Memory -> stackFrame.takeArgument(pos.index, pos.size)
                null -> null
            }
            allocation.add(operand)
        }

        return allocation
    }

    companion object {
        private val gpRegisters = CallConvention.gpArgumentRegisters
        private val fpRegisters = CallConvention.xmmArgumentRegister

        fun alloc(stackFrame: StackFrame, arguments: List<Value>): List<Operand?> {
            return CalleeArgumentAllocator(stackFrame, arguments).calculate()
        }
    }
}