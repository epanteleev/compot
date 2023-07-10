package ir.codegen

import asm.*
import ir.*
import ir.utils.LiveIntervals
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

class LinearScan(private var stackSize: Long, private val registerMap: MutableMap<Value, Operand>, val countOfCalleeSaveRegisters: Int) {
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

    fun get(value: Value): Operand {
        if (value is ValueInstruction || value is ArgumentValue) {
            return registerMap[value] as Operand
        }
        return when (value) {
            is U8Value  -> Imm(value.u8.toLong(), 1)
            is I8Value  -> Imm(value.i8.toLong(), 1)
            is U16Value -> Imm(value.u16.toLong(), 2)
            is I16Value -> Imm(value.i16.toLong(), 2)
            is U32Value -> Imm(value.u32.toLong(), 4)
            is I32Value -> Imm(value.i32.toLong(), 4)
            is I64Value -> Imm(value.i64, 8)
            is U64Value -> Imm(value.u64, 8)
            else -> throw RuntimeException("expect $value:${value.type()}")
        }
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

        private fun allocRegistersForArguments(registerMap: MutableMap<Value, Operand>, data: FunctionData, stackSize: Long) {
            val allocator = ArgumentRegisterAllocator(stackSize)
            for (argument in data.arguments()) {
                registerMap[argument] = allocator.allocNext(argument)
            }
        }

        private fun allocRegistersForLocalVariables(registerMap: MutableMap<Value, Operand>, liveRanges: LiveIntervals): Long {
            val allocator = RegisterAllocator()
            for ((v, _) in liveRanges) {
                registerMap[v] = allocator.allocNext(v)
            }

            return allocator.stackSize()
        }

        private fun countOfUsedCalleeSaveRegisters(registerMap: Map<Value, Operand>): Int {
            var count = 0
            for (operand in registerMap.values) {
                if (gpNonVolatileRegs.contains(operand)) {
                    count += 1
                }
            }

            return count
        }

        fun alloc(data: FunctionData): LinearScan {
            val liveRanges = data.blocks.liveness()
            val registerMap = hashMapOf<Value, Operand>()
            val stackSize = allocRegistersForLocalVariables(registerMap, liveRanges)

            val baseSize = 0x10L /** sizeof(ret addr) + sizeof(rbp) */
            val count = countOfUsedCalleeSaveRegisters(registerMap)
            allocRegistersForArguments(registerMap, data, count * 8 + baseSize)

            return LinearScan(stackSize, registerMap, count)
        }
    }
}