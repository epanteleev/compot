package ir.platform.regalloc

import asm.x64.Address
import asm.x64.GPRegister.*
import asm.x64.Operand
import ir.Value
import ir.platform.x64.CallConvention

class CalleeArgumentAllocator(private val arguments: List<Value>) {
    private interface Place
    private data class Memory(val index: Int): Place
    private data class RealRegister(val registerIndex: Int): Place

    private fun emit(index: Int): Place {
        return if (index < registers.size) {
            RealRegister(index)
        } else {
            Memory(index - registers.size)
        }
    }

    fun calculate(): List<Operand> {
        val allocation = arrayListOf<Operand>()
        val places = arguments.indices.mapTo(arrayListOf()) { emit(it) }

        for ((pos, value) in places.zip(arguments)) {
            when (pos) {
                is RealRegister -> {
                    val reg = registers[pos.registerIndex]
                    allocation.add(reg)
                }
                is Memory -> {
                    allocation.add(Address.mem(rsp, -(8L * pos.index) + 8))
                }
            }
        }

        return allocation
    }

    companion object {
        private val registers = CallConvention.gpArgumentRegisters

        fun alloc(arguments: List<Value>): List<Operand> {
            return CalleeArgumentAllocator(arguments).calculate()
        }
    }
}