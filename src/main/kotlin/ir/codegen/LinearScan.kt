package ir.codegen

import asm.*
import ir.*
import java.lang.RuntimeException
import java.lang.StringBuilder

private class ArgumentRegisterAllocator(val stackSize: Long) {
    private var index: Long = 0

    fun allocNext(value: ArgumentValue): Operand {
        val size = LinearScan.gpArgumentRegisters.size
        val slot = if (index < size) {
            LinearScan.gpArgumentRegisters[index.toInt()]
        } else {
            Mem(Rbp(8), stackSize + 8L * (index - size), 8)
        }
        index += 1

        return slot
    }
}

private class RegisterAllocator {
    private var index: Long = 0
    private var stackSize: Long = 0

    fun allocNext(value: Value): Operand {
        if (value is StackAlloc) {
            val typeSize = value.type().size()
            val totalSize = typeSize * value.size
            stackSize += totalSize
            return Mem(Rbp(8), -stackSize, typeSize)
        }

        val size = LinearScan.availableRegisters.size
        val slot = if (index < size) {
            LinearScan.availableRegisters[index.toInt()]
        } else {
            val typeSize = value.type().size()
            stackSize += typeSize
            Mem(Rbp(8), -stackSize, typeSize)
        }
        index += 1

        return slot
    }

    fun stackSize(): Long {
        return stackSize
    }
}

class LinearScan(private val data: FunctionData) {
    private val registerMap = hashMapOf<Value, Operand>()
    private val liveRanges = data.blocks.liveness()
    private var stackSize: Long

    init {
        stackSize = allocRegistersForLocalVariables()

        val baseSize = 0x10L /** sizeof(ret addr) + sizeof(rbp)*/
        allocRegistersForArguments(calleeSaveRegisters().size * 8 + baseSize)
    }

    fun reservedStackSize(): Long {
        return stackSize
    }

    fun calleeSaveRegisters(): List<GPRegister> {
        val registers = arrayListOf<GPRegister>()
        for (reg in registerMap.values) {
            if (reg is GPRegister && gpNonVolatileRegs.contains(reg)) {
                registers.add(reg)
            }
        }

        return registers
    }

    fun callerSaveRegisters(): List<GPRegister> {
        val registers = arrayListOf<GPRegister>()
        for (reg in registerMap.values) {
            if (reg is GPRegister && gpVolatileRegs.contains(reg)) {
                registers.add(reg)
            }
        }

        return registers
    }

    fun usedArgumentRegisters(): List<GPRegister> {
        val registers = arrayListOf<GPRegister>()
        for (reg in registerMap.values) {
            if (reg is GPRegister && gpArgumentRegisters.contains(reg)) {
                registers.add(reg)
            }
        }

        return registers
    }

    fun get(value: Value): Operand {
        if (value is Instruction || value is ArgumentValue) {
            return registerMap[value] as Operand
        }
        TODO()
       // val imm = Imm()
    }

    private fun allocRegistersForArguments(stackSize: Long) {
        val allocator = ArgumentRegisterAllocator(stackSize)

        for (argument in data.arguments()) {
            registerMap[argument] = allocator.allocNext(argument)
        }
    }

    private fun allocRegistersForLocalVariables(): Long {
        val allocator = RegisterAllocator()
        for ((v, _) in liveRanges) {
            registerMap[v] = allocator.allocNext(v)
        }

        return allocator.stackSize()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for ((v, operand) in registerMap) {
            builder.append("$v -> $operand ")
        }

        return builder.toString()
    }

    companion object {
        internal val gpArgumentRegisters = arrayOf(
            Rdi(8),
            Rsi(8),
            Rdx(8),
            Rcx(8),
            R8(8),
            R9(8)
        )

        internal val gpVolatileRegs = arrayOf(
            Rax(8),
            Rcx(8),
            Rdx(8),
            Rsi(8),
            Rdi(8),
            R8(8),
            R9(8),
            R10(8),
            R11(8)
        )

        internal val gpNonVolatileRegs = arrayOf(
            Rbp(8),
            Rbx(8),
            R12(8),
            R13(8),
            R14(8),
            R15(8)
        )

        fun basePointer(): GPRegister {
            return Rbp(8)
        }

        fun temp1(size: Int): GPRegister {
            return Rax(size)
        }

        fun temp2(size: Int): GPRegister {
            return Rcx(size)
        }

        private fun allRegisters(): ArrayList<GPRegister> {
            val allRegisters = arrayListOf<GPRegister>()
            allRegisters.addAll(gpVolatileRegs)
            allRegisters.addAll(gpNonVolatileRegs)

            allRegisters.remove(basePointer())
            allRegisters.removeAll(gpArgumentRegisters.toSet())

            return allRegisters
        }

        internal val availableRegisters = allRegisters()

        fun alloc(data: FunctionData): LinearScan {
            return LinearScan(data)
        }
    }
}