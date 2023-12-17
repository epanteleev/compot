package ir.platform.x64.utils

import asm.x64.*
import ir.platform.x64.CodeEmitter
import ir.platform.x64.utils.Utils.case
import ir.instruction.ArithmeticBinaryOp


object AddCodegen {
    operator fun invoke(objFunc: ObjFunction, dst: AnyOperand, first: AnyOperand, second: AnyOperand, size: Int) {
        when {
            case<GPRegister, GPRegister, GPRegister>(dst, first, second) -> {
                first as GPRegister
                dst as GPRegister
                second as GPRegister
                if (first == dst) {
                    objFunc.add(size, second, dst)
                } else if (second == dst) {
                    objFunc.add(size, first, dst)
                } else {
                    objFunc.mov(size, first, dst)
                    objFunc.add(size, second, dst)
                }
            }

            case<Address, GPRegister, GPRegister>(dst, first, second) -> {
                dst as Address
                second as Register
                objFunc.mov(size, first, dst)
                objFunc.add(size, second, dst)
            }

            case<Address, Address, GPRegister>(dst, first, second) -> {
                dst as Address
                first as Address
                second as GPRegister
                objFunc.mov(size, first, CodeEmitter.temp1)
                objFunc.add(size, second, CodeEmitter.temp1)
                objFunc.mov(size, CodeEmitter.temp1, dst)
            }

            case<GPRegister, Imm, GPRegister>(dst, first, second) -> {
                dst as GPRegister
                first as Imm
                second as GPRegister
                if (dst == second) {
                    objFunc.add(size, first, dst)
                } else {
                    objFunc.lea(size, Address.mem(second, first.value), dst)
                }
            }

            case<GPRegister, GPRegister, Imm>(dst, first, second) -> {
                dst as GPRegister
                first as GPRegister
                second as Imm
                if (dst == first) {
                    objFunc.add(size, second, dst)
                } else {
                    objFunc.lea(size, Address.mem(first, second.value), dst)
                }
            }

            case<Address, Address, Address>(dst, first, second) -> {
                dst as Address
                first as Address
                second as Address
                objFunc.mov(size, first, CodeEmitter.temp1)
                objFunc.add(size, second, CodeEmitter.temp1)
                objFunc.mov(size, CodeEmitter.temp1, dst)
            }

            case<GPRegister, Address, GPRegister>(dst, first, second) -> {
                dst as GPRegister
                first as Address
                second as GPRegister

                if (dst == second) {
                    objFunc.add(size, first, second)
                } else {
                    objFunc.mov(size, first, CodeEmitter.temp1)
                    objFunc.add(size, second, CodeEmitter.temp1)
                    objFunc.mov(size, CodeEmitter.temp1, dst)
                }
            }

            case<Address, GPRegister, GPRegister>(dst, first, second) -> {
                dst as Address
                second as Register
                objFunc.mov(size, first, dst)
                objFunc.add(size, second, dst)
            }

            else -> throw RuntimeException("Unimplemented: '${ArithmeticBinaryOp.Add}' dst=$dst, first=$first, second=$second")
        }
    }
}