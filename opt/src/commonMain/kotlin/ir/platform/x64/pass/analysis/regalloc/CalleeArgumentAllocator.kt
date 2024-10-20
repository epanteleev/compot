package ir.platform.x64.pass.analysis.regalloc

import asm.Operand
import ir.value.Value
import asm.x64.*
import ir.types.*
import ir.platform.x64.CallConvention


class CalleeArgumentAllocator private constructor(private val stackFrame: StackFrame, private val arguments: List<Value>) {
    private sealed interface Place
    private data class Memory(val index: Int): Place
    private data class RealGPRegister(val registerIndex: Int): Place
    private data class RealFpRegister(val registerIndex: Int): Place

    private var gpRegPos = 0
    private var xmmRegPos = 0
    private var memSlots = 0

    private fun emit(type: Type): Place? = when (type) {
        is FloatingPointType -> {
            if (xmmRegPos < fpRegisters.size) {
                xmmRegPos += 1
                RealFpRegister(xmmRegPos - 1)
            } else {
                memSlots += 1
                Memory(memSlots - 1)
            }
        }
        is IntegerType, is PointerType, is FlagType -> {
            if (gpRegPos < gpRegisters.size) {
                gpRegPos += 1
                RealGPRegister(gpRegPos - 1)
            } else {
                memSlots += 1
                Memory(memSlots - 1)
            }
        }
        is UndefType -> null
        else -> throw IllegalArgumentException("type=$type")
    }


    private fun calculate(): List<Operand?> {
        val allocation = arrayListOf<Operand?>()
        for (arg in arguments) {
            val operand = when (val pos = emit(arg.type())) {
                is RealGPRegister -> gpRegisters[pos.registerIndex]
                is RealFpRegister -> fpRegisters[pos.registerIndex]
                is Memory -> stackFrame.takeArgument(pos.index, arg)
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