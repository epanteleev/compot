package ir.platform.x64.utils

import asm.x64.*
import ir.types.*
import ir.platform.x64.CallConvention
import ir.platform.x64.utils.Utils.case


object NotCodegen {
    operator fun invoke(type: IntegerType, objFunc: ObjFunction, dst: AnyOperand, operand: AnyOperand) {
        generate(objFunc, dst, operand, type.size())
    }

    private fun generate(objFunc: ObjFunction, dst: AnyOperand, operand: AnyOperand, size: Int) {
        when {
            case<GPRegister, GPRegister>(dst, operand) -> {
                dst     as GPRegister
                operand as GPRegister
                if (dst == operand) {
                    objFunc.not(size, dst)
                } else {
                    objFunc.mov(size, operand, dst)
                    objFunc.not(size, dst)
                }
            }

            case<GPRegister, ImmInt>(dst, operand) -> {
                dst     as GPRegister
                operand as ImmInt
                objFunc.mov(size, ImmInt(operand.value.inv()), dst)
            }

            case<GPRegister, Address>(dst, operand) -> {
                dst     as GPRegister
                operand as Address
                objFunc.mov(size, operand, dst)
                objFunc.not(size, dst)
            }

            case<Address, ImmInt>(dst, operand) -> {
                dst     as Address
                operand as ImmInt
                objFunc.mov(size, ImmInt(operand.value.inv()), dst)
            }

            case<Address, GPRegister>(dst, operand) -> {
                dst     as Address
                operand as GPRegister
                objFunc.mov(size, operand, dst)
                objFunc.not(size, dst)
            }

            case<Address, Address>(dst, operand) -> {
                dst     as Address
                operand as Address

                if (dst == operand) {
                    objFunc.not(size, dst)
                } else {
                    objFunc.mov(size, operand, CallConvention.temp1)
                    objFunc.not(size, CallConvention.temp1)
                    objFunc.mov(size, CallConvention.temp1, dst)
                }
            }

            else -> throw RuntimeException("Unimplemented: dst=$dst, operand=$operand")
        }
    }

}