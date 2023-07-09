package ir.codegen

import asm.*
import ir.*
import ir.Call

private class ArgumentEmitter(val objFunc: ObjFunction) {
    private var index: Long = 0

    fun allocNext(value: Operand) {
        val size = LinearScan.gpArgumentRegisters.size
        if (index < size) {
            objFunc.mov(LinearScan.temp1(value.size), LinearScan.gpArgumentRegisters[index.toInt()])
        } else {
            if (value is Mem) {
                objFunc.mov(value, LinearScan.temp1(value.size))
                objFunc.push(LinearScan.temp1(value.size))
            } else {
                objFunc.push(value as GPRegister)
            }
        }

        index += 1
    }
}

class CodeEmitter(val data: FunctionData, private val objFunc: ObjFunction) {
    private val valueToRegister = LinearScan.alloc(data)

    private fun temp1(size: Int): GPRegister {
        return LinearScan.temp2(size)
    }

    private fun temp2(size: Int): GPRegister {
        return LinearScan.temp2(size)
    }

    private fun emitPrologue() {
        val stackSize = valueToRegister.reservedStackSize()
        if (stackSize != 0L) {
            objFunc.push(Rbp(8))
        }

        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters()
        for (reg in calleeSaveRegisters) {
            objFunc.push(reg)
        }

        if (stackSize != 0L) {
            objFunc.mov(Rsp(8), Rbp(8))
            objFunc.sub(Imm(stackSize, 8), Rsp(8))
        }
    }

    private fun emitEpilogue() {
        val stackSize = valueToRegister.reservedStackSize()
        if (stackSize != 0L) {
            objFunc.add(Imm(stackSize, 8), Rsp(8))
            objFunc.mov(Rbp(8), Rsp(8))
        }

        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters()
        for (reg in calleeSaveRegisters) {
            objFunc.pop(reg)
        }

        if (stackSize != 0L) {
            objFunc.pop(Rbp(8))
        }
    }

    private fun emitArithmeticBinary(binary: ArithmeticBinary) {
        var first       = valueToRegister.get(binary.first())
        var second      = valueToRegister.get(binary.second())
        val destination = valueToRegister.get(binary)

        if (first is Mem) {
            first = objFunc.mov(first, temp1(first.size))
        }

        if (second is Mem) {
            second = objFunc.mov(second, temp2(second.size))
        } else {
            if (destination is Register) {
                second = objFunc.mov(second, destination)
            }
        }

        when (binary.op) {
            ArithmeticBinaryOp.Add -> {
                objFunc.add(first, second as Register)
            }
            ArithmeticBinaryOp.Sub -> {
                objFunc.sub(first, second as Register)
            }
            ArithmeticBinaryOp.Xor -> {
                objFunc.xor(first, second as Register)
            }
            else -> {
                TODO()
            }
        }

        if (destination is Mem) {
            objFunc.mov(second, destination)
        }

    }

    private fun emitReturn(ret: Return) {
        val returnType = data.prototype.returnType
        if (returnType.isArithmetic() || returnType.isPointer()) {
            val value = valueToRegister.get(ret.value())
            objFunc.mov(value, temp1(value.size))
        }

        emitEpilogue()
        objFunc.ret()
    }

    private fun emitBasicBlock(bb: BasicBlock) {
        for (instruction in bb) {
            when (instruction) {
                is ArithmeticBinary -> emitArithmeticBinary(instruction)
                is Store            -> emitStore(instruction)
                is Return           -> emitReturn(instruction)
                is Load             -> emitLoad(instruction)
                is Call             -> emitCall(instruction)
            }
        }
    }

    private fun emitCall(call: Call) {
        val arguments = valueToRegister.usedArgumentRegisters()
        for (arg in arguments) {
            objFunc.push(arg)
        }

        val argEmitter = ArgumentEmitter(objFunc)

        for (arg in call.arguments()) {
            argEmitter.allocNext(valueToRegister.get(arg))
        }

        objFunc.call(call.func.name)

        for (arg in arguments) {
            objFunc.pop(arg)
        }
    }

    private fun emitStore(instruction: Store) {
        val pointer = valueToRegister.get(instruction.pointer())
        var value = valueToRegister.get(instruction.value())

        if (value is Mem) {
            value = objFunc.mov(value, temp2(value.size))
        }

        objFunc.mov(value, pointer)
    }

    private fun emitLoad(instruction: Load) {
        val pointer = valueToRegister.get(instruction.operand())
        val value = valueToRegister.get(instruction)

        objFunc.mov(pointer, value)
    }

    private fun emit() {
        emitPrologue()
        for (bb in data.blocks.preorder()) {
            emitBasicBlock(bb)
        }
    }

    companion object {
        fun codegen(module: Module): Assembler {
            val asm = Assembler()

            for ((fn, data) in module.functions) {
                CodeEmitter(data, asm.mkFunction(fn.name)).emit()
            }
            return asm
        }
    }
}