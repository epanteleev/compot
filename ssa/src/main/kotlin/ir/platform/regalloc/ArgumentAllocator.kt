package ir.platform.regalloc

import ir.Value
import asm.x64.*
import asm.x64.GPRegister.*
import ir.platform.x64.CallConvention
import ir.types.*
import java.lang.IllegalArgumentException


class CalleeArgumentAllocator(private val arguments: List<Value>) {
    private interface Place
    private data class Memory(val index: Int, val slotSize: Long): Place
    private data class RealGPRegister(val registerIndex: Int): Place
    private data class RealFpRegister(val registerIndex: Int): Place

    private var gpRegPos = 0
    private var xmmRegPos = 0
    private var memSlots = 0

    private fun emit(type: Type): Place {
        return when (type) {
            is FloatingPointType -> {
                if (xmmRegPos < fpRegisters.size) {
                    xmmRegPos += 1
                    RealFpRegister(xmmRegPos - 1)
                } else {
                    memSlots += 1
                    Memory(memSlots - 1, 16) //TODO 4 or 8???
                }
            }
            is IntegerType, is PointerType, is BooleanType -> {
                if (gpRegPos < gpRegisters.size) {
                    gpRegPos += 1
                    RealGPRegister(gpRegPos - 1)
                } else {
                    memSlots += 1
                    Memory(memSlots - 1, 8)
                }
            }
            else -> throw IllegalArgumentException("type=$type")
        }
    }

    fun calculate(): List<Operand> {
        val allocation = arrayListOf<Operand>()
        val places = arguments.mapTo(arrayListOf()) { arg -> emit(arg.type()) }

        for (pos in places) {
            when (pos) {
                is RealGPRegister -> {
                    allocation.add(gpRegisters[pos.registerIndex])
                }
                is RealFpRegister -> {
                    allocation.add(fpRegisters[pos.registerIndex])
                }
                is Memory -> {
                    allocation.add(Address.mem(rsp, -(pos.slotSize * pos.index) + 8))
                }
            }
        }

        return allocation
    }

    companion object {
        private val gpRegisters = CallConvention.gpArgumentRegisters
        private val fpRegisters = CallConvention.xmmArgumentRegister

        fun alloc(arguments: List<Value>): List<Operand> {
            return CalleeArgumentAllocator(arguments).calculate()
        }
    }
}