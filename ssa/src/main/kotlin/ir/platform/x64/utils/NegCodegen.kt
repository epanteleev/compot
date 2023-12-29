package ir.platform.x64.utils

import asm.x64.*
import ir.types.*
import asm.x64.Imm.Companion.minusZero
import ir.platform.x64.utils.Utils.case
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1


object NegCodegen {
    operator fun invoke(type: PrimitiveType, objFunc: ObjFunction, dst: AnyOperand, operand: AnyOperand) {
        when (type) {
            is FloatingPointType -> generateForFp(objFunc, dst, operand, type.size())
            is IntegerType   -> generate(objFunc, dst, operand, type.size())
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, operand=$operand")
        }
    }

    private fun generate(objFunc: ObjFunction, dst: AnyOperand, operand: AnyOperand, size: Int) {
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

            case<GPRegister, ImmInt>(dst, operand) -> {
                dst     as GPRegister
                operand as ImmInt
                objFunc.mov(size, ImmInt(-operand.value), dst)
            }

            case<GPRegister, Address>(dst, operand) -> {
                dst     as GPRegister
                operand as Address
                objFunc.mov(size, operand, dst)
                objFunc.neg(size, dst)
            }

            case<Address, ImmInt>(dst, operand) -> {
                dst     as Address
                operand as ImmInt
                objFunc.mov(size, ImmInt(-operand.value), dst)
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

    private fun generateForFp(objFunc: ObjFunction, dst: AnyOperand, operand: AnyOperand, size: Int) {
        when {
            case<XmmRegister, XmmRegister>(dst, operand) -> {
                dst     as XmmRegister
                operand as XmmRegister

                objFunc.mov(size, minusZero(size), temp1)
                objFunc.movd(size, temp1, xmmTemp1)
                if (dst == operand) {
                    objFunc.xorpf(size, xmmTemp1, dst)
                } else {
                    objFunc.xorpf(size, operand, xmmTemp1)
                    objFunc.movf(size, xmmTemp1, dst)
                }
            }

            case<XmmRegister, ImmFp>(dst, operand) -> {
                dst     as XmmRegister
                operand as ImmFp

                objFunc.mov(size, (-operand).bits(), temp1)
                objFunc.movd(size, temp1, dst)
            }

            case<XmmRegister, Address>(dst, operand) -> {
                dst     as XmmRegister
                operand as Address

                objFunc.mov(size, Imm.minusZero(size), temp1)
                objFunc.movd(size, temp1, xmmTemp1)
                objFunc.xorpf(size, operand, xmmTemp1)
                objFunc.movf(size, xmmTemp1, dst)
            }

            case<Address, ImmFp>(dst, operand) -> {
                dst     as Address
                operand as ImmFp

                objFunc.mov(size, (-operand).bits(), temp1)
                objFunc.movd(size, temp1, xmmTemp1)
                objFunc.movf(size, xmmTemp1, dst)
            }

            case<Address, XmmRegister>(dst, operand) -> {
                dst     as Address
                operand as XmmRegister

                objFunc.mov(size, minusZero(size), temp1)
                objFunc.movd(size, temp1, xmmTemp1)
                objFunc.xorpf(size, operand, xmmTemp1)
                objFunc.movf(size, xmmTemp1, dst)
            }

            case<Address, Address>(dst, operand) -> {
                dst     as Address
                operand as Address

                objFunc.mov(size, minusZero(size), temp1)
                objFunc.movd(size, temp1, xmmTemp1)
                objFunc.xorpf(size, operand, xmmTemp1)
                objFunc.movf(size, xmmTemp1, dst)
            }

            else -> throw RuntimeException("Unimplemented: dst=$dst, operand=$operand")
        }
    }
}