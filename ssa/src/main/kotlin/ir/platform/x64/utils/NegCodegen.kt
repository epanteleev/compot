package ir.platform.x64.utils

import asm.x64.*
import ir.types.*
import ir.platform.x64.utils.Utils.case
import ir.platform.x64.CallConvention.temp1


data class NegCodegen(val type: PrimitiveType, val objFunc: ObjFunction) {
    private val size: Int = type.size()

    operator fun invoke(dst: AnyOperand, operand: AnyOperand) {
        when (type) {
            is IntegerType -> generate(objFunc, dst, operand)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, operand=$operand")
        }
    }

    private fun generate(objFunc: ObjFunction, dst: AnyOperand, operand: AnyOperand) {
        when {
            case<GPRegister, GPRegister>(dst, operand) -> {
                dst     as GPRegister
                operand as GPRegister
                if (dst == operand) {
                    objFunc.neg(size, dst)
                } else {
                    objFunc.mov(size, operand, dst)
                    objFunc.neg(size, dst)
                }
            }

            case<GPRegister, Imm32>(dst, operand) -> {
                dst     as GPRegister
                operand as ImmInt
                objFunc.mov(size, Imm32(-operand.value()), dst)
            }

            case<GPRegister, Address>(dst, operand) -> {
                dst     as GPRegister
                operand as Address
                objFunc.mov(size, operand, dst)
                objFunc.neg(size, dst)
            }

            case<Address, Imm32>(dst, operand) -> {
                dst     as Address
                operand as Imm32
                objFunc.mov(size, Imm32(-operand.value()), dst)
            }

            case<Address, GPRegister>(dst, operand) -> {
                dst     as Address
                operand as GPRegister
                objFunc.mov(size, operand, dst)
                objFunc.neg(size, dst)
            }

            case<Address, Address>(dst, operand) -> {
                dst     as Address
                operand as Address

                if (dst == operand) {
                    objFunc.neg(size, dst)
                } else {
                    objFunc.mov(size, operand, temp1)
                    objFunc.neg(size, temp1)
                    objFunc.mov(size, temp1, dst)
                }
            }

            else -> throw RuntimeException("Unimplemented: dst=$dst, operand=$operand")
        }
    }
}