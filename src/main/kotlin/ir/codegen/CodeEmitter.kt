package ir.codegen

import asm.*
import ir.*
import ir.Call
import ir.utils.Location

private class ArgumentEmitter(val objFunc: ObjFunction) {
    private var index: Long = 0

    fun emit(value: AnyOperand) {
        val size = CallConvention.gpArgumentRegisters.size
        val old = index
        index += 1

        if (old < size) {
            objFunc.mov(value, CallConvention.gpArgumentRegisters[old.toInt()])
            return
        }

        when (value) {
            is Mem -> {
                objFunc.mov(value, CodeEmitter.temp1(value.size))
                objFunc.push(CodeEmitter.temp1(value.size))
            }
            is GPRegister -> objFunc.push(value)
            is Imm        -> objFunc.push(value)
            else -> throw RuntimeException("Internal error")
        }
    }
}

class CodeEmitter(val data: FunctionData, private val objFunc: ObjFunction) {
    private val valueToRegister = LinearScan.alloc(data)

    private fun emitPrologue() {
        val stackSize = valueToRegister.reservedStackSize()
        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters
        for (reg in calleeSaveRegisters) {
            objFunc.push(reg)
            objFunc.mov(Rsp.rsp, Rbp.rbp)
        }

        if (stackSize != 0L) {
            objFunc.sub(Imm(stackSize, 8), Rsp.rsp)
        }
    }

    private fun emitEpilogue() {
        val stackSize = valueToRegister.reservedStackSize()
        if (stackSize != 0L) {
            objFunc.add(Imm(stackSize, 8), Rsp.rsp)
        }

        val calleeSaveRegisters = valueToRegister.calleeSaveRegisters
        for (reg in calleeSaveRegisters) {
            objFunc.pop(reg)
        }
    }

    private fun emitArithmeticBinary(binary: ArithmeticBinary) {
        var first       = valueToRegister.get(binary.first())
        var second      = valueToRegister.get(binary.second())
        val destination = valueToRegister.get(binary) as Operand

        if (first is Mem) {
            first = objFunc.mov(first, temp1(first.size))
        }

        second = if (destination is Mem) {
            objFunc.mov(second, temp2(second.size))
        } else {
            objFunc.mov(second, destination)
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
            ArithmeticBinaryOp.Mul -> {
                objFunc.mul(first, second as Register)
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
        val returnType = data.prototype.type()
        if (returnType.isArithmetic() || returnType.isPointer()) {
            val value = valueToRegister.get(ret.value())
            objFunc.mov(value, temp1(value.size))
        }

        emitEpilogue()
        objFunc.ret()
    }

    private fun emitArithmeticUnary(unary: ArithmeticUnary) {
        val operand = valueToRegister.get(unary.operand())
        val result  = valueToRegister.get(unary)

        if (unary.op == ArithmeticUnaryOp.Neg) {
            val second = if (operand is Mem) {
                objFunc.mov(operand, temp1(8))
            } else {
                operand as Register
            }
            objFunc.xor(Imm(-1, 8), second)
            objFunc.mov(second, result as Operand)

        } else if (unary.op == ArithmeticUnaryOp.Not) {
            val second = if (result is Mem) {
                objFunc.mov(result, temp1(8))
            } else {
                result as Register
            }
            objFunc.xor(second, second)
            objFunc.sub(operand, second)

            if (result is Mem) {
                objFunc.mov(temp1(8), result)
            }

        } else {
            throw RuntimeException("Internal error")
        }

    }

    private fun emitCall(call: Call, location: Location) {
        val arguments = valueToRegister.callerSaveRegisters(location)
        for (arg in arguments) {
            objFunc.push(arg)
        }

        val argEmitter = ArgumentEmitter(objFunc)

        for (arg in call.arguments()) {
            argEmitter.emit(valueToRegister.get(arg))
        }

        objFunc.call(call.func.name)

        for (arg in arguments.reversed()) {
            objFunc.pop(arg)
        }
    }

    private fun emitStore(instruction: Store) {
        val pointer = valueToRegister.get(instruction.pointer()) as Operand
        var value   = valueToRegister.get(instruction.value())

        if (value is Mem) {
            value = objFunc.mov(value, temp2(value.size))
        }

        objFunc.mov(value, pointer)
    }

    private fun emitLoad(instruction: Load) {
        val pointer = valueToRegister.get(instruction.operand())
        val value   = valueToRegister.get(instruction) as Operand

        objFunc.mov(pointer, value)
    }

    private fun emitBasicBlock(bb: BasicBlock) {
        for ((index, instruction) in bb.withIndex()) {
            when (instruction) {
                is ArithmeticBinary -> emitArithmeticBinary(instruction)
                is Store            -> emitStore(instruction)
                is Return           -> emitReturn(instruction)
                is Load             -> emitLoad(instruction)
                is Call             -> emitCall(instruction, Location(bb, index))
                is ArithmeticUnary  -> emitArithmeticUnary(instruction)
            }
        }
    }

    private fun emit() {
        emitPrologue()
        for (bb in data.blocks.preorder()) {
            emitBasicBlock(bb)
        }
    }

    companion object {
        fun temp1(size: Int): GPRegister {
            return CallConvention.temp1(size)
        }

        fun temp2(size: Int): GPRegister {
            return CallConvention.temp2(size)
        }

        fun codegen(module: Module): Assembler {
            val asm = Assembler()

            for (data in module.functions()) {
                CodeEmitter(data, asm.mkFunction(data.prototype.name)).emit()
            }
            return asm
        }
    }
}