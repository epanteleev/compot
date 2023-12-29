package ir.platform.x64.utils

import asm.x64.*
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.utils.Utils.case
import ir.types.*

object CopyCodegen {
    operator fun invoke(type: PrimitiveType, objFunc: ObjFunction, dst: AnyOperand, origin: AnyOperand) {
        when (type) {
            is FloatingPointType -> generateForFp(objFunc, dst, origin, type.size())
            is IntegerType, is PointerType -> generate(objFunc, dst, origin, type.size())
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, origin=$origin")
        }
    }

    private fun generate(objFunc: ObjFunction, dst: AnyOperand, origin: AnyOperand, size: Int)  {
        when {
            case<GPRegister, GPRegister>(dst, origin) -> {
                dst    as GPRegister
                origin as GPRegister
                if (dst == origin) {
                    return
                }

                objFunc.mov(size, origin, dst)
            }

            case<GPRegister, AddressLiteral>(dst, origin) -> {
                dst    as GPRegister
                origin as AddressLiteral
                objFunc.lea(size, origin, dst)
            }

            case<GPRegister, Address>(dst, origin) -> {
                dst    as GPRegister
                origin as Address
                objFunc.mov(size, origin, dst)
            }

            case<GPRegister, Imm32>(dst, origin) -> {
                dst    as GPRegister
                origin as Imm32
                objFunc.mov(size, origin, dst)
            }

            case<Address, Imm32>(dst, origin) -> {
                dst    as Address
                origin as Imm32
                objFunc.mov(size, origin, dst)
            }

            case<Address, GPRegister>(dst, origin) -> {
                dst    as Address
                origin as GPRegister
                objFunc.mov(size, origin, dst)
            }

            case<Address, AddressLiteral>(dst, origin) -> {
                dst    as Address
                origin as AddressLiteral
                objFunc.mov(size, origin, temp1)
                objFunc.mov(size, temp1, dst)
            }

            case<Address, Address>(dst, origin) -> {
                dst    as Address
                origin as Address
                objFunc.mov(size, origin, temp1)
                objFunc.mov(size, temp1, dst)
            }

            else -> throw RuntimeException("Unimplemented: 'load' dst=$dst, origin=$origin")
        }
    }

    private fun generateForFp(objFunc: ObjFunction, dst: AnyOperand, origin: AnyOperand, size: Int) {
        when {
            case<XmmRegister, XmmRegister>(dst, origin) -> {
                dst    as XmmRegister
                origin as XmmRegister
                if (dst == origin) {
                    return
                }

                objFunc.movf(size, origin, dst)
            }

            case<XmmRegister, AddressLiteral>(dst, origin) -> {
                dst    as GPRegister
                origin as AddressLiteral
                TODO()
            }

            case<XmmRegister, Address>(dst, origin) -> {
                dst    as XmmRegister
                origin as Address
                objFunc.movf(size, origin, dst)
            }

            case<Address, XmmRegister>(dst, origin) -> {
                dst    as Address
                origin as XmmRegister
                objFunc.movf(size, origin, dst)
            }

            case<Address, AddressLiteral>(dst, origin) -> {
                dst    as Address
                origin as AddressLiteral
                TODO()
            }

            case<Address, Address>(dst, origin) -> {
                dst    as Address
                origin as Address
                objFunc.movf(size, origin, xmmTemp1)
                objFunc.movf(size, xmmTemp1, dst)
            }

            else -> throw RuntimeException("Unimplemented: dst=$dst, origin=$origin")
        }
    }
}