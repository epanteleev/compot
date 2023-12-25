package ir.platform.x64.utils

import asm.x64.*
import ir.types.*
import ir.platform.x64.CodeEmitter
import ir.platform.x64.utils.Utils.case
import ir.instruction.ArithmeticBinaryOp
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1


object AddCodegen {
    operator fun invoke(type: PrimitiveType, objFunc: ObjFunction, dst: AnyOperand, first: AnyOperand, second: AnyOperand) {
        when (type) {
            is FloatingPoint  -> generateForFp(objFunc, dst, first, second, type.size())
            is ArithmeticType -> generate(objFunc, dst, first, second, type.size())
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
    }

    private fun generate(objFunc: ObjFunction, dst: AnyOperand, first: AnyOperand, second: AnyOperand, size: Int) {
        when {
            case<GPRegister, GPRegister, GPRegister>(dst, first, second) -> {
                first  as GPRegister
                dst    as GPRegister
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
                dst    as Address
                first  as GPRegister
                second as GPRegister
                objFunc.mov(size, first, dst)
                objFunc.add(size, second, dst)
            }

            case<Address, Address, GPRegister>(dst, first, second) -> {
                dst    as Address
                first  as Address
                second as GPRegister
                objFunc.mov(size, first, CodeEmitter.temp1)
                objFunc.add(size, second, CodeEmitter.temp1)
                objFunc.mov(size, CodeEmitter.temp1, dst)
            }

            case<GPRegister, ImmInt, GPRegister>(dst, first, second) -> {
                dst    as GPRegister
                first  as ImmInt
                second as GPRegister
                if (dst == second) {
                    objFunc.add(size, first, dst)
                } else {
                    objFunc.lea(size, Address.mem(second, first.value), dst)
                }
            }

            case<GPRegister, GPRegister, ImmInt>(dst, first, second) -> {
                dst    as GPRegister
                first  as GPRegister
                second as ImmInt
                if (dst == first) {
                    objFunc.add(size, second, dst)
                } else {
                    objFunc.lea(size, Address.mem(first, second.value), dst)
                }
            }

            case<Address, Address, Address>(dst, first, second) -> {
                dst    as Address
                first  as Address
                second as Address
                objFunc.mov(size, first, CodeEmitter.temp1)
                objFunc.add(size, second, CodeEmitter.temp1)
                objFunc.mov(size, CodeEmitter.temp1, dst)
            }

            case<GPRegister, Address, GPRegister>(dst, first, second) -> {
                dst    as GPRegister
                first  as Address
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
                dst    as Address
                first  as GPRegister
                second as GPRegister
                objFunc.mov(size, first, dst)
                objFunc.add(size, second, dst)
            }

            case<GPRegister, ImmInt, ImmInt>(dst, first, second) -> {
                dst    as GPRegister
                first  as ImmInt
                second as ImmInt
                objFunc.mov(size, ImmInt(first.value + second.value), dst)
            }

            case<Address, ImmInt, ImmInt>(dst, first, second) -> {
                dst    as Address
                first  as ImmInt
                second as ImmInt
                objFunc.mov(size, ImmInt(first.value + second.value), dst)
            }

            else -> throw RuntimeException("Unimplemented: '${ArithmeticBinaryOp.Add}' dst=$dst, first=$first, second=$second")
        }
    }

    private fun generateForFp(objFunc: ObjFunction, dst: AnyOperand, first: AnyOperand, second: AnyOperand, size: Int) {
        when {
            case<XmmRegister, XmmRegister, XmmRegister>(dst, first, second) -> {
                first  as XmmRegister
                dst    as XmmRegister
                second as XmmRegister
                if (first == dst) {
                    objFunc.addf(size, second, dst)
                } else if (second == dst) {
                    objFunc.addf(size, first, dst)
                } else {
                    objFunc.movf(size, first, dst)
                    objFunc.addf(size, second, dst)
                }
            }

            case<Address, XmmRegister, XmmRegister>(dst, first, second) -> {
                dst    as Address
                first  as XmmRegister
                second as XmmRegister
                objFunc.movf(size, first, xmmTemp1)
                objFunc.addf(size, second, xmmTemp1)
                objFunc.movf(size, xmmTemp1, dst)
            }

            case<Address, Address, XmmRegister>(dst, first, second) -> {
                dst    as Address
                first  as Address
                second as XmmRegister
                objFunc.movf(size, first, xmmTemp1)
                objFunc.addf(size, second, xmmTemp1)
                objFunc.movf(size, xmmTemp1, dst)
            }

            case<GPRegister, ImmFp, XmmRegister>(dst, first, second) -> {
                dst    as XmmRegister
                first  as ImmFp
                second as XmmRegister
                objFunc.mov(size, first.bits(), temp1)
                objFunc.movd(size, temp1, xmmTemp1)

                if (dst == second) {
                    objFunc.addf(size, xmmTemp1, dst)
                } else {
                    objFunc.addf(size, second, xmmTemp1)
                    objFunc.movf(size, xmmTemp1, dst)
                }
            }

            case<XmmRegister, XmmRegister, ImmFp>(dst, first, second) -> {
                dst    as XmmRegister
                first  as XmmRegister
                second as ImmFp
                objFunc.mov(size, second.bits(), temp1)
                objFunc.movd(size, temp1, xmmTemp1)

                if (dst == first) {
                    objFunc.addf(size, xmmTemp1, dst)
                } else {
                    objFunc.addf(size, first, xmmTemp1)
                    objFunc.movf(size, xmmTemp1, dst)
                }
            }

            case<Address, Address, Address>(dst, first, second) -> {
                dst    as Address
                first  as Address
                second as Address
                objFunc.movf(size, first, xmmTemp1)
                objFunc.addf(size, second, xmmTemp1)
                objFunc.movf(size, xmmTemp1, dst)
            }

            case<XmmRegister, Address, XmmRegister>(dst, first, second) -> {
                dst    as XmmRegister
                first  as Address
                second as XmmRegister
                if (second == dst) {
                    objFunc.addf(size, first, dst)
                } else {
                    objFunc.movf(size, first, xmmTemp1)
                    objFunc.addf(size, second, xmmTemp1)
                    objFunc.movf(size, xmmTemp1, dst)
                }
            }

            case<Address, XmmRegister, XmmRegister>(dst, first, second) -> {
                dst    as Address
                first  as XmmRegister
                second as XmmRegister
                objFunc.movf(size, first, xmmTemp1)
                objFunc.addf(size, second, xmmTemp1)
                objFunc.movf(size, xmmTemp1, dst)
            }

            case<XmmRegister, ImmFp, ImmFp>(dst, first, second) -> {
                dst    as XmmRegister
                first  as ImmFp
                second as ImmFp
                objFunc.mov(size, (first + second).bits(), temp1)
                objFunc.movd(size, temp1, dst)
            }

            case<Address, ImmFp, ImmFp>(dst, first, second) -> {
                dst    as Address
                first  as ImmFp
                second as ImmFp
                objFunc.mov(size, (first + second).bits(), dst)
            }

            else -> throw RuntimeException("Unimplemented: '${ArithmeticBinaryOp.Add}' dst=$dst, first=$first, second=$second")
        }
    }
}