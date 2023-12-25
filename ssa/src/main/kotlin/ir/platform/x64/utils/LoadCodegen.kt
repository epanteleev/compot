package ir.platform.x64.utils

import asm.x64.*
import ir.types.*
import ir.platform.x64.utils.Utils.case
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1


object LoadCodegen {
    operator fun invoke(type: PrimitiveType, objFunc: ObjFunction, dst: AnyOperand, pointer: AnyOperand) {
        when (type) {
            is FloatingPoint               -> generateForFp(objFunc, dst, pointer, type.size())
            is IntegerType, is PointerType -> generate(objFunc, dst, pointer, type.size())
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, pointer=$pointer")
        }
    }

    private fun generate(objFunc: ObjFunction, dst: AnyOperand, pointer: AnyOperand, size: Int)  {
        when {
            case<GPRegister, GPRegister>(dst, pointer) -> {
                dst     as GPRegister
                pointer as GPRegister
                objFunc.mov(size, Address.mem(pointer, 0), dst)
            }

            case<GPRegister, Address>(dst, pointer) -> {
                dst     as GPRegister
                pointer as Address
                objFunc.mov(size, pointer, dst)
            }

            case<Address, Address>(dst, pointer) -> {
                dst     as Address
                pointer as Address
                objFunc.mov(size, pointer, temp1)
                objFunc.mov(size, temp1, dst)
            }

            else -> throw RuntimeException("Unimplemented: 'load' dst=$dst, pointer=$pointer")
        }
    }

    private fun generateForFp(objFunc: ObjFunction, dst: AnyOperand, pointer: AnyOperand, size: Int) {
        when {
            case<XmmRegister, GPRegister>(dst, pointer) -> {
                dst     as XmmRegister
                pointer as GPRegister
                objFunc.movf(size, Address.mem(pointer, 0), dst)
            }

            case<XmmRegister, Address>(dst, pointer) -> {
                dst     as XmmRegister
                pointer as Address
                objFunc.movf(size, pointer, dst)
            }

            case<Address, Address>(dst, pointer) -> {
                dst     as Address
                pointer as Address
                objFunc.movf(size, pointer, xmmTemp1)
                objFunc.movf(size, xmmTemp1, dst)
            }

            else -> throw RuntimeException("Unimplemented: dst=$dst, pointer=$pointer")
        }
    }
}