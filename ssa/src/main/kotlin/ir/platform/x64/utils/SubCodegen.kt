package ir.platform.x64.utils

import asm.x64.*
import ir.instruction.ArithmeticBinaryOp
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.CodeEmitter
import ir.platform.x64.utils.Utils.case

object SubCodegen {
    operator fun invoke(objFunc: ObjFunction, dst: AnyOperand, first: AnyOperand, second: AnyOperand, size: Int) {
        when {
            case<GPRegister, GPRegister, GPRegister>(dst, first, second) -> {
                first as GPRegister
                dst as GPRegister
                second as GPRegister
                when {
                    (first == dst)  -> objFunc.sub(size, second, dst)
                    (second == dst) -> {
                        //TODO
                        objFunc.mov(size, second, temp1)
                        objFunc.mov(size, first, temp2)
                        objFunc.sub(size, temp1, temp2)
                        objFunc.mov(size, temp2, dst)
                    }
                    else -> {
                        objFunc.mov(size, first, dst)
                        objFunc.sub(size, second, dst)
                    }
                }
            }

            case<Address, GPRegister, GPRegister>(dst, first, second) -> {
                dst as Address
                second as Register
                objFunc.mov(size, first, dst)
                objFunc.sub(size, second, dst)
            }

            case<Address, Address, GPRegister>(dst, first, second) -> {
                dst as Address
                first as Address
                second as GPRegister
                objFunc.mov(size, first, temp1)
                objFunc.sub(size, second, CodeEmitter.temp1)
                objFunc.mov(size, temp1, dst)
            }

            case<GPRegister, Imm, GPRegister>(dst, first, second) -> {
                dst as GPRegister
                first as Imm
                second as GPRegister

                objFunc.mov(size, first, temp1)
                objFunc.sub(size, second, CodeEmitter.temp1)
                objFunc.mov(size, temp1, dst)
            }

            case<Address, Address, Address>(dst, first, second) -> {
                dst as Address
                first as Address
                second as Address
                objFunc.mov(size, first, temp1)
                objFunc.sub(size, second, temp1)
                objFunc.mov(size, temp1, dst)
            }

            case<GPRegister, GPRegister, Imm>(dst, first, second) -> {
                dst as GPRegister
                first as GPRegister
                second as Imm

                if (dst == first) {
                    objFunc.sub(size, second, dst)
                } else {
                    objFunc.lea(size, Address.mem(first, -second.value), dst)
                }
            }

            case<GPRegister, Address, GPRegister>(dst, first, second) -> {
                dst as GPRegister
                first as Address
                second as GPRegister

                objFunc.mov(size, first, temp1)
                objFunc.sub(size, second, temp1)
                objFunc.mov(size, temp1, dst)
            }

            case<Address, GPRegister, GPRegister>(dst, first, second) -> {
                dst as Address
                second as Register
                objFunc.mov(size, first, dst)
                objFunc.sub(size, second, dst)
            }

            else -> throw RuntimeException("Unimplemented: '${ArithmeticBinaryOp.Sub}' dst=$dst, first=$first, second=$second")
        }
    }
}