package ir.platform.x64.pass.analysis.regalloc

import asm.x64.VReg
import common.assertion
import ir.Definitions
import ir.value.Value
import ir.Definitions.QWORD_SIZE
import ir.instruction.lir.Generate
import ir.types.*
import ir.platform.x64.CallConvention


internal class CallerArgumentAllocator private constructor(private val stackFrame: StackFrame, private val arguments: List<Value>) {
    private var gpRegPos = 0
    private var xmmRegPos = 0
    private var memSlots = 0

    private fun peakFPArgument(): VReg {
        if (xmmRegPos < fpRegisters.size) {
            xmmRegPos += 1
            return fpRegisters[xmmRegPos - 1]
        } else {
            memSlots += 1
            return stackFrame.takeArgument((memSlots - 1) * QWORD_SIZE)
        }
    }

    private fun peakGPArgument(): VReg {
        if (gpRegPos < gpRegisters.size) {
            gpRegPos += 1
            return gpRegisters[gpRegPos - 1]
        } else {
            memSlots += 1
            return stackFrame.takeArgument((memSlots - 1) * QWORD_SIZE)
        }
    }

    private fun peakStructArgument(value: Value): VReg {
        assertion(value is Generate) { "value=$value" }
        value as Generate
        val size = value.type().sizeOf()
        val slot = memSlots
        memSlots += Definitions.alignTo(size, QWORD_SIZE) / QWORD_SIZE
        return stackFrame.takeArgument(slot * QWORD_SIZE)
    }

    private fun emit(value: Value): VReg? = when (value.type()) {
        is FloatingPointType       -> peakFPArgument()
        is IntegerType, is PtrType -> peakGPArgument()
        is AggregateType           -> peakStructArgument(value)
        is UndefType               -> null
        else -> throw IllegalArgumentException("type=$value")
    }

    private fun calculate(): List<VReg?> {
        val allocation = arrayListOf<VReg?>()
        for (arg in arguments) {
            allocation.add(emit(arg))
        }

        return allocation
    }

    companion object {
        private val gpRegisters = CallConvention.gpArgumentRegisters
        private val fpRegisters = CallConvention.xmmArgumentRegisters

        fun alloc(stackFrame: StackFrame, arguments: List<Value>): List<VReg?> {
            return CallerArgumentAllocator(stackFrame, arguments).calculate()
        }
    }
}